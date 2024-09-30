attribute vec4 aPosition;
attribute vec4 aTextureCoordinate;
uniform mat4 uMVPMatrix;
uniform float uAspectRatio;
varying vec2 vTextureCoord;

void main() {
    gl_Position = uMVPMatrix * aPosition;

    // 调整纹理坐标以实现裁切
    vec2 texCoord = aTextureCoordinate.xy;
    texCoord = (texCoord - 0.5) * 2.0;
    texCoord.x = texCoord.x * min(uAspectRatio, 1.0);
    texCoord.y = texCoord.y * min(1.0 / uAspectRatio, 1.0);
    texCoord = texCoord * 0.5 + 0.5;

    vTextureCoord = texCoord;
}