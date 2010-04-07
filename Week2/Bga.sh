#define SCALE 1.0f
#define SPHERESIZE 1.0f * SCALE / 2.0
#define CONEBASE 0.5f * SCALE
#define CONETOP 0.25f * SCALE
#define CONEHEIGHT 2.0f * SCALE
#define GRIDSPACING 1.5f * SCALE

#define GRIDWIDTH 16
#define GRIDHEIGHT 16
#define LASERCOUNT 512
#define LASERHEIGHT 4.0f / 2.0f

typedef struct Laser_s {
	bool Active;
	float X, Y, Z, Traj;
	float Width;
	float Color[3];
} Laser;

class Bga {
public:
	float Position[3];
	
	Bga(float x, float z);
	void RenderSolid();
	void RenderTranslucent();
	void Update();
	void Note(int note, int volume);
	
	GLuint GridTex[4];
	float GridMod[GRIDWIDTH*GRIDHEIGHT];
	float AddBuf[GRIDWIDTH*GRIDHEIGHT*4];
	
	Laser Lasers[LASERCOUNT];
	bool FromTop;
	
	GLuint fbo;
};
