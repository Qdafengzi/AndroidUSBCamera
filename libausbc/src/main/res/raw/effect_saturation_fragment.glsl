varying highp vec2 vTextureCoord;

uniform sampler2D uTextureSampler;
uniform lowp float saturation;

// Values from "Graphics Shaders: Theory and Practice" by Bailey and Cunningham
const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);

void main()
{
     lowp vec4 textureColor = texture2D(uTextureSampler, vTextureCoord);
     lowp float luminance = dot(textureColor.rgb, luminanceWeighting);
     lowp vec3 greyScaleColor = vec3(luminance);

     gl_FragColor = vec4(mix(greyScaleColor, textureColor.rgb, saturation), textureColor.w);
}