precision mediump float;
varying vec2 vTextureCoord;
uniform sampler2D uTextureSampler;

void main() {
    // 检查是否在裁切区域内
    if (vTextureCoord.x < 0.0 || vTextureCoord.x > 1.0 ||
    vTextureCoord.y < 0.0 || vTextureCoord.y > 1.0) {
        discard; // 丢弃超出裁切区域的片段
    } else {
        gl_FragColor = texture2D(uTextureSampler, vTextureCoord);
    }
}