varying highp vec2 vTextureCoord;

uniform sampler2D uTextureSampler;
uniform lowp float contrast;

void main() {
     lowp vec4 textureColor = texture2D(uTextureSampler, vTextureCoord);

     gl_FragColor = vec4(((textureColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), textureColor.w);
}