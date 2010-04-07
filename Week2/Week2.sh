% import 'Sidt/Sidt.sh'
% import 'Week2/Room.sh'

typedef struct Textures_s {
	GLuint aluminum;
	GLuint wall;
} Textures_t;
%define %w{Textures_t Textures}

typedef struct Shaders_s {
	GLuint wave, spheres, laser;
} Shaders_t;
%define %w{Shaders_t Shaders}

void gentextures();
void genshaders();

void setView(int order);
