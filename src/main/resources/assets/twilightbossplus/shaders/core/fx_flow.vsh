#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec4 flowParams;
out vec3 shapeParams;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * Position, FogShape);
    vertexColor = Color;
    texCoord0 = UV0;
    // UV1：溶解度与亮边宽度（千分比）；UV2：噪声滚动速度（偏移 16000，千分比）
    flowParams = vec4(float(UV1.x) / 1000.0, float(UV1.y) / 1000.0, float(UV2.x - 16000) / 1000.0, float(UV2.y - 16000) / 1000.0);
    // 法线：热核心强度、噪声缩放、流动强度
    shapeParams = Normal;
}
