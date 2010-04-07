% import 'Week2/Bga.sh'
#include <bassmod.h>

#define ROOMW 150.0f / 2.0f
#define ROOMH 40.0f / 2.0f
#define ROOMD 150.0f / 2.0f

class Room {
public:
	int BgaCount;
	Bga **Bgas;
	
	Room();
	void Render();
	void Update();
};
