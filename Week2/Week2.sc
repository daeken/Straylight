% import 'Week2/Week2.sh'

% $demotitle = 'Armitage by Straylight'
% $fullscreen = true
% $windowSize = [800, 600]
% $bgcolor = [0.0, 0.0, 0.0, 1.0]
% $defaultResize = true

Room *room;
float CameraPosition[3], CameraRotation[3];

void SetCamera(
	float x, float y, float z, 
	float rx, float ry, float rz
) {
	CameraPosition[0] = x;
	CameraPosition[1] = y;
	CameraPosition[2] = z;
	CameraRotation[0] = rx;
	CameraRotation[1] = ry;
	CameraRotation[2] = rz;
}

void init() {
	srand(0xDEADBEEF);
	
	glClearColor(<%= $bgcolor.params %>);
	glClearDepth(1.0);
	glDepthFunc(GL_LESS);
	glEnable(GL_DEPTH_TEST);
	glShadeModel(GL_SMOOTH);
	
	glBlendFunc(GL_SRC_ALPHA, GL_ONE);
	glEnable(GL_BLEND);
	glEnable(GL_TEXTURE_2D);
	
	glLoadIdentity();
	<%
		light :GL_LIGHT0 do
			position 0.0, 0.0, 10.0, 1.0
			specular 1.0, 1.0, 1.0, 1.0
			ambient 1.0, 1.0, 1.0
			enable
		end
	%>
	
	glEnable(GL_LIGHTING);
	glEnable(GL_COLOR_MATERIAL);
	glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
	glMateriali(GL_FRONT, GL_SHININESS, 128);
	
	gentextures();
	genshaders();
	
	room = new Room();
	checkErrors
	
	setView(0);
}

void update() {
}

void setView(int order) {
	switch(order) {
		case 1:
			SetCamera(
				0.0f, -ROOMH + CONEHEIGHT, 0.0f, 
				90.0f, 0.0f, 0.0f
			);
			break;
		case 8:
		case 2:
			SetCamera(
				-ROOMW, 0.0, -ROOMD, 
				0.0f, -45.0f, 0.0f
			);
			break;
		case 7:
		case 3:
			SetCamera(
				-ROOMW*2/3, 12.0, ROOMD*2/3, 
				-20.0f, -140.0f, 0.0f
			);
			break;
		case 6:
		case 4:
			SetCamera(
				0.0, -20.0, ROOMD, 
				30.0f, 180.0f, 0.0f
			);
			break;
		default:
			SetCamera(
				70.0f, -20.0f, -50.0f, 
				30.0f, 50.0f, 0.0f
			);
			break;
	}
}

void render() {
	glLoadIdentity();
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	
	glRotatef(CameraRotation[0], 1.0, 0.0, 0.0);
	glRotatef(CameraRotation[1], 0.0, 1.0, 0.0);
	glRotatef(CameraRotation[2], 0.0, 0.0, 1.0);
	glTranslatef(CameraPosition[0], CameraPosition[1], CameraPosition[2]);
	
	checkErrors
	room->Render();
	checkErrors
	room->Update();
	checkErrors
}

void quit() {
	BASSMOD_MusicStop();
}
