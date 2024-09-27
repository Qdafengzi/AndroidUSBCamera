precision highp float;

varying highp vec2 vTextureCoord;
varying highp vec2 leftTextureCoordinate;
varying highp vec2 rightTextureCoordinate;
varying highp vec2 topTextureCoordinate;
varying highp vec2 bottomTextureCoordinate;

varying highp float centerMultiplier;
varying highp float edgeMultiplier;

uniform sampler2D uTextureSampler;

void main()
{
     mediump vec3 textureColor = texture2D(uTextureSampler, vTextureCoord).rgb;
     mediump vec3 leftTextureColor = texture2D(uTextureSampler, leftTextureCoordinate).rgb;
     mediump vec3 rightTextureColor = texture2D(uTextureSampler, rightTextureCoordinate).rgb;
     mediump vec3 topTextureColor = texture2D(uTextureSampler, topTextureCoordinate).rgb;
     mediump vec3 bottomTextureColor = texture2D(uTextureSampler, bottomTextureCoordinate).rgb;

     gl_FragColor = vec4((textureColor * centerMultiplier -(leftTextureColor * edgeMultiplier + rightTextureColor * edgeMultiplier + topTextureColor * edgeMultiplier + bottomTextureColor * edgeMultiplier)), texture2D(uTextureSampler, bottomTextureCoordinate).w);
}