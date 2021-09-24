#version 330 core
//
// Pass through Vertex shader.  
// Passes vertex information through without changing it.
//  Being used for debugging purposes.
// 
uniform mat4 modelingMatrix;
uniform mat4 viewingMatrix;
uniform mat4 projectionMatrix;

in vec4 vColor;
in vec4 vPosition;

out vec4 Color;

void main()
{
	Color = vColor;
    gl_Position = projectionMatrix * viewingMatrix * modelingMatrix * vPosition;
}