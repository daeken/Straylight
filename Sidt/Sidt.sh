#include <windows.h>

% import "Sidt/Math.sh"
% import "Sidt/Gl.sh"

//#define DEBUG
#ifdef DEBUG
#include <stdio.h>
#endif

#ifdef DEBUG
#define checkErrors { \
	GLenum err = glGetError(); \
	if(err != GL_NO_ERROR) \
		printf("GL error (%s:%i): %s\n", __FILE__, __LINE__, gluErrorString(err)); \
}
#define validate(program) { \
	int ret; \
	glValidateProgram(program); \
	glGetProgramiv(program, GL_VALIDATE_STATUS, &ret); \
	if(ret != GL_TRUE) { \
		char buf[65536]; \
		int len; \
		printf("Program validation failed at (%s:%i):\n", __FILE__, __LINE__); \
		glGetProgramInfoLog(program, 65535, &len, buf); \
		printf("%s\n", buf); \
	} \
}
#else
#define checkErrors
#define validate(program)
#endif

void init();
void render();
void update();
void resize(int width, int height);
void quit();
void finish();
