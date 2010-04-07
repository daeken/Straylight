% import 'Week2/Week2.sh'

GLuint roomList;

void setupRoom() {
	roomList = glGenLists(1);
	
	glNewList(roomList, GL_COMPILE);
	glBegin(GL_QUADS);
	
	// Ceiling
	drawQuad(
			0.0, ROOMH, 0.0, 
			ROOMW, 0.0, ROOMD
		);
	
	// Floor
	drawQuad(
			0.0, -ROOMH, 0.0, 
			ROOMW, 0.0, ROOMD
		);
	
	// Back
	drawQuad(
			0.0, 0.0, -ROOMD, 
			ROOMW, ROOMH, 0.0
		);
	
	// Front
	drawQuad(
			0.0, 0.0, ROOMD, 
			ROOMW, ROOMH, 0.0
		);
	
	// Left
	drawQuad(
			-ROOMW, 0.0, 0.0, 
			0.0, ROOMH, ROOMD
		);
	
	// Right
	drawQuad(
			ROOMW, 0.0, 0.0, 
			0.0, ROOMH, ROOMD
		);
	
	glEnd();
	glEndList();
}

typedef struct InstData_s {
	Room *room;
	int instrument;
} InstData;

void CALLBACK InstCallback(HSYNC handle, DWORD cdata, DWORD user) {
	InstData *data = (InstData *) user;
	data->room->Bgas[(data->instrument + (data->room->BgaCount-1)) % data->room->BgaCount]->Note(LOWORD(cdata), HIWORD(cdata));
}

void CALLBACK EOMCallback(HSYNC handle, DWORD cdata, DWORD user) {
	finish();
}

void CALLBACK PosCallback(HSYNC handle, DWORD cdata, DWORD user) {
	setView(cdata & 0xFFFF);
}

unsigned char mod[] = <%= File.open('Week2/lynx-daeken.mod', 'rb').read.to_carray %>;
Room::Room() {
	setupRoom();
	
	BgaCount = 5;
	Bgas = new Bga *[BgaCount];
	Bgas[0] = new Bga(-30.0, 0.0);
	Bgas[1] = new Bga(0.0, 0.0);
	Bgas[2] = new Bga(30.0, 0.0);
	Bgas[3] = new Bga(0.0, -30.0);
	Bgas[4] = new Bga(0.0, 30.0);
	
	BASSMOD_Init(-1, 44100, 0);
	BASSMOD_MusicLoad(TRUE, mod, 0, 0, 0);
	
	for(int i = 1; i <= 64; ++i) {
		InstData *data = (InstData *) malloc(sizeof(InstData));
		data->room = this;
		data->instrument = i-1;
		BASSMOD_MusicSetSync(BASS_SYNC_MUSICINST, i | 0xFFFF0000U, InstCallback, (DWORD) data);
	}
	
	BASSMOD_MusicSetSync(BASS_SYNC_END, 0, EOMCallback, 0);
	BASSMOD_MusicSetSync(BASS_SYNC_POS, 0x0000FFFFU, PosCallback, 0);
	
	BASSMOD_MusicPlay();
}

void Room::Render() {
	GLuint loc;
	glBindTexture(GL_TEXTURE_2D, Textures.wall);
	glCallList(roomList);
	
	checkErrors
	for(int i = 0; i < BgaCount; ++i)
		Bgas[i]->RenderSolid();
	checkErrors
	
	for(int i = 0; i < BgaCount; ++i)
		Bgas[i]->RenderTranslucent();
	checkErrors
}

void Room::Update() {
	for(int i = 0; i < BgaCount; ++i)
		Bgas[i]->Update();
}
