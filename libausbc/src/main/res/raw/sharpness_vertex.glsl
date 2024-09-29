attribute vec4 aPosition;
attribute vec4 aTextureCoordinate;

uniform float imageWidthFactor;
uniform float imageHeightFactor;
uniform float sharpness;

varying highp vec2 vTextureCoord;
varying highp vec2 leftTextureCoordinate;
varying highp vec2 rightTextureCoordinate;
varying highp vec2 topTextureCoordinate;
varying highp vec2 bottomTextureCoordinate;

varying highp float centerMultiplier;
varying highp float edgeMultiplier;

void main()
{
    gl_Position = aPosition;

    highp vec2 widthStep = vec2(imageWidthFactor, 0.0);
    highp vec2 heightStep = vec2(0.0, imageHeightFactor);

    vTextureCoord = aTextureCoordinate.xy;
    leftTextureCoordinate = aTextureCoordinate.xy - widthStep;
    rightTextureCoordinate = aTextureCoordinate.xy + widthStep;
    topTextureCoordinate = aTextureCoordinate.xy + heightStep;
    bottomTextureCoordinate = aTextureCoordinate.xy - heightStep;

    centerMultiplier = 1.0 + 4.0 * sharpness;
    edgeMultiplier = sharpness;
}