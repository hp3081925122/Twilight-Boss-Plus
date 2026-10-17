#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform float GameTime;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 flowParams;
in vec3 shapeParams;

out vec4 fragColor;

void main() {
    vec4 mask = texture(Sampler0, texCoord0);
    // 游戏时间换算为秒，避免一天循环时跳变过大
    float seconds = GameTime * 1200.0;
    float scale = max(shapeParams.y * 8.0, 0.25);
    vec2 scroll = flowParams.zw * seconds;
    float n1 = texture(Sampler1, texCoord0 * scale + scroll).r;
    float n2 = texture(Sampler1, texCoord0 * scale * 0.57 - scroll * 0.63 + vec2(0.37, 0.71)).g;
    float noise = n1 * 0.6 + n2 * 0.4;
    float flow = clamp(shapeParams.z, 0.0, 1.0);
    float shape = mask.a * mix(1.0, 0.3 + noise * 1.4, flow);
    // 溶解：噪声低于阈值的部分被切掉，切口附近发亮
    float dissolve = flowParams.x;
    float edgeWidth = max(flowParams.y, 0.001);
    float cut = noise - dissolve;
    float dissolving = step(0.0005, dissolve);
    if (dissolving > 0.5 && cut < 0.0) {
        discard;
    }
    float edgeGlow = dissolving * (1.0 - smoothstep(0.0, edgeWidth, cut));
    // 热核心：越亮越偏白
    float hot = clamp(shapeParams.x, 0.0, 1.0);
    float core = pow(clamp(shape, 0.0, 1.0), 2.0) * hot;
    vec3 rgb = mix(vertexColor.rgb * mask.rgb, vec3(1.0), clamp(core + edgeGlow * 0.85, 0.0, 1.0));
    float alpha = clamp(shape + edgeGlow * mask.a, 0.0, 1.0) * vertexColor.a;
    vec4 color = vec4(rgb, alpha) * ColorModulator;
    fragColor = color * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
