precision highp float;

varying vec2 vTextureCoord;
varying vec2 leftTextureCoordinate;
varying vec2 rightTextureCoordinate;
varying vec2 topTextureCoordinate;
varying vec2 bottomTextureCoordinate;

varying float centerMultiplier;
varying float edgeMultiplier;

uniform sampler2D uTextureSampler;

void main()
{
     mediump vec3 textureColor = texture2D(uTextureSampler, vTextureCoord).rgb;
     mediump vec3 leftTextureColor = texture2D(uTextureSampler, leftTextureCoordinate).rgb;
     mediump vec3 rightTextureColor = texture2D(uTextureSampler, rightTextureCoordinate).rgb;
     mediump vec3 topTextureColor = texture2D(uTextureSampler, topTextureCoordinate).rgb;
     mediump vec3 bottomTextureColor = texture2D(uTextureSampler, bottomTextureCoordinate).rgb;

     mediump vec3 resultColor = textureColor * centerMultiplier - (
     leftTextureColor * edgeMultiplier +
     rightTextureColor * edgeMultiplier +
     topTextureColor * edgeMultiplier +
     bottomTextureColor * edgeMultiplier
     );
     gl_FragColor = vec4(resultColor, texture2D(uTextureSampler, vTextureCoord).a);
}