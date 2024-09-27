varying highp vec2 vTextureCoord;
uniform sampler2D uTextureSampler;
uniform mediump vec3 levelMinimum;
uniform mediump vec3 levelMiddle;
uniform mediump vec3 levelMaximum;
uniform mediump vec3 minOutput;
uniform mediump vec3 maxOutput;

void main()
{
 mediump vec4 textureColor = texture2D(uTextureSampler, vTextureCoord);
 gl_FragColor = vec4(
 mix(
 minOutput,
 maxOutput,
 pow(min(max(textureColor.rgb - levelMinimum, vec3(0.0)) / (levelMaximum - levelMinimum),vec3(1.0)),1.0 / levelMiddle)),textureColor.a);
}