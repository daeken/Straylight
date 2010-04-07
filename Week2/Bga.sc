% import 'Week2/Week2.sh'

GLuint sphereList = 0xFFFFFFFFU;
GLuint coneList = 0xFFFFFFFFU;

void bgaSetup() {
	GLUquadricObj *quadric = gluNewQuadric();
	gluQuadricNormals(quadric, GLU_SMOOTH);
	gluQuadricTexture(quadric, GL_TRUE);
	
	sphereList = glGenLists(1);
	glNewList(sphereList, GL_COMPILE);
	gluSphere(quadric, SPHERESIZE, 32, 32);
	glEndList();
	
	coneList = glGenLists(1);
	glNewList(coneList, GL_COMPILE);
	for(int y = 0; y < GRIDHEIGHT; ++y) {
		for(int x = 0; x < GRIDWIDTH; ++x) {
			drawCylinder(
					CONEBASE, 
					CONETOP, 
					CONEHEIGHT, 
					(x - GRIDWIDTH / 2.0f) * GRIDSPACING, 
					0.0, 
					(y - GRIDHEIGHT / 2.0f) * GRIDSPACING, 
					32
				);
		}
	}
	glEndList();
	
	gluDeleteQuadric(quadric);
}

Bga::Bga(float x, float z) {
	Position[0] = x;
	Position[1] = z;
	
	if(sphereList == 0xFFFFFFFFU)
		bgaSetup();
	
	glGenTextures(4, GridTex);
	for(int i = 0; i < 3; ++i) {
		glBindTexture(GL_TEXTURE_2D, GridTex[i]);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, GRIDWIDTH, GRIDHEIGHT, 0, GL_RGBA, GL_FLOAT, NULL);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	}
	glBindTexture(GL_TEXTURE_2D, GridTex[3]);
	memset(AddBuf, 0, sizeof(float) * GRIDHEIGHT * GRIDWIDTH * 4);
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, GRIDWIDTH, GRIDHEIGHT, 0, GL_RGBA, GL_FLOAT, AddBuf);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	
	memset(Lasers, 0, sizeof(Laser) * LASERCOUNT);
}

void Bga::RenderSolid() {
	glBindTexture(GL_TEXTURE_2D, Textures.aluminum);
	
	glPushMatrix();
	glTranslatef(Position[0], -ROOMH, Position[1]);
	glCallList(coneList);
	
	glTranslatef(0.0, ROOMH*2.0, 0.0);
	glRotatef(180.0f, 1.0f, 0.0f, 0.0f);
	glCallList(coneList);
	glPopMatrix();
}

void Bga::RenderTranslucent() {
	glEnable(GL_BLEND);
	glDisable(GL_DEPTH_TEST);
	glPushMatrix();
	checkErrors
	glBindTexture(GL_TEXTURE_2D, GridTex[0]);
	checkErrors
	validate(Shaders.spheres)
	glUseProgram(Shaders.spheres);
	checkErrors
	glTranslatef(-(GRIDWIDTH / 2.0) * GRIDSPACING + Position[0], 0.0, -(GRIDHEIGHT / 2.0) * GRIDSPACING + Position[1]);
	glDisable(GL_TEXTURE_2D);
	for(int y = 0; y < GRIDHEIGHT; ++y) {
		float yc = y / (float) (GRIDHEIGHT - 1);
		for(int x = 0; x < GRIDWIDTH; ++x) {
			glColor4f(x / (float) (GRIDWIDTH - 1), yc, 0.0, 0.0);
			glCallList(sphereList);
			glTranslatef(GRIDSPACING, 0.0, 0.0);
		}
		glTranslatef(-GRIDWIDTH * GRIDSPACING, 0.0, GRIDSPACING);
	}
	glColor4f(1.0, 1.0, 1.0, 1.0);
	glPopMatrix();
	
	glUseProgram(Shaders.laser);
	for(int i = 0; i < LASERCOUNT; ++i) {
		if(!Lasers[i].Active)
			continue;
		
		Laser *laser = &Lasers[i];
		glColor3f(laser->Color[0], laser->Color[1], laser->Color[2]);
		glBegin(GL_QUADS);
		drawQuad(
				Position[0] + (-(GRIDWIDTH / 2.0) + laser->X) * GRIDSPACING, 
				laser->Y, 
				Position[1] + (-(GRIDHEIGHT / 2.0) + laser->Z) * GRIDSPACING, 
				laser->Width, LASERHEIGHT, 0.0
			);
		glEnd();
		
		laser->Y += laser->Traj;
		
		if(laser->Y < -ROOMH + CONEHEIGHT + LASERHEIGHT || laser->Y > ROOMH - CONEHEIGHT - LASERHEIGHT)
			laser->Active = FALSE;
	}
	glColor4f(1.0, 1.0, 1.0, 1.0);
	glUseProgram(0);
	glDisable(GL_BLEND);
	glEnable(GL_DEPTH_TEST);
	glEnable(GL_TEXTURE_2D);
}

void Bga::Update() {
	GLuint fbo, loc, temp = GridTex[2];
	GridTex[2] = GridTex[1];
	GridTex[1] = GridTex[0];
	GridTex[0] = temp;
	
	checkErrors
	glGenFramebuffers(1, &fbo);
	glBindFramebuffer(GL_FRAMEBUFFER, fbo);
	glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, GridTex[0], 0);
	checkErrors
	
	glDisable(GL_DEPTH_TEST);
	glDisable(GL_LIGHTING);
	glDisable(GL_TEXTURE_2D);
	
	checkErrors
	glPushAttrib(GL_VIEWPORT_BIT);
	
	checkErrors
	glMatrixMode(GL_PROJECTION);
	glPushMatrix();
	glLoadIdentity();
	
	checkErrors
	glMatrixMode(GL_MODELVIEW);
	glPushMatrix();
	glLoadIdentity();
	glViewport(0, 0, GRIDWIDTH, GRIDHEIGHT);
	
	checkErrors
	glActiveTexture(GL_TEXTURE0);
	glBindTexture(GL_TEXTURE_2D, GridTex[1]);
	checkErrors
	glActiveTexture(GL_TEXTURE1);
	glBindTexture(GL_TEXTURE_2D, GridTex[2]);
	checkErrors
	glActiveTexture(GL_TEXTURE2);
	glBindTexture(GL_TEXTURE_2D, GridTex[3]);
	checkErrors
	glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, GRIDWIDTH, GRIDHEIGHT, 0, GL_RGBA, GL_FLOAT, AddBuf);
	
	checkErrors
	validate(Shaders.wave)
	checkErrors
	glUseProgram(Shaders.wave);
	
	glColor4f(1.0, 1.0, 1.0, 1.0);
	glBegin(GL_QUADS);
	glTexCoord2f(0.0, 0.0); glVertex3f(-1.0, -1.0, 0.0);
	glTexCoord2f(1.0, 0.0); glVertex3f( 1.0, -1.0, 0.0);
	glTexCoord2f(1.0, 1.0); glVertex3f( 1.0,  1.0, 0.0);
	glTexCoord2f(0.0, 1.0); glVertex3f(-1.0,  1.0, 0.0);
	glEnd();
	
	checkErrors
	glPopMatrix();
	glMatrixMode(GL_PROJECTION);
	glPopMatrix();
	glMatrixMode(GL_MODELVIEW);
	
	checkErrors
	glActiveTexture(GL_TEXTURE0);
	glUseProgram(0);
	
	checkErrors
	glEnable(GL_DEPTH_TEST);
	glEnable(GL_LIGHTING);
	glEnable(GL_TEXTURE_2D);
	
	checkErrors
	glPopAttrib();
	glBindFramebuffer(GL_FRAMEBUFFER, 0);
	glDeleteFramebuffers(1, &fbo);
	checkErrors
	
	memset(AddBuf, 0, sizeof(float) * GRIDHEIGHT * GRIDWIDTH * 4);
}

void Bga::Note(int note, int volume) {
	Laser *laser = NULL;
	int pos = ((GRIDHEIGHT * GRIDWIDTH - 1 - note) * 2) % (GRIDWIDTH * GRIDHEIGHT);
	
	for(int i = 0; i < LASERCOUNT; ++i) {
		if(!Lasers[i].Active) {
			laser = &Lasers[i];
			laser->Active = TRUE;
			break;
		}
	}
	
	if(laser == NULL)
		laser = &Lasers[0];
	
	laser->Width = (float) volume / 64;
	laser->X = pos % GRIDWIDTH;
	laser->Z = pos / GRIDWIDTH;
	if(FromTop) {
		laser->Y = ROOMH - CONEHEIGHT - LASERHEIGHT;
		laser->Traj = -3.0f;
	} else {
		laser->Y = -ROOMH + CONEHEIGHT + LASERHEIGHT;
		laser->Traj = 3.0f;
	}
	laser->Color[0] = laser->X / (GRIDWIDTH - 1);
	laser->Color[1] = laser->Z / (GRIDHEIGHT - 1);
	laser->Color[2] = 1.0;
	
	if(FromTop)
		volume = -volume;
	AddBuf[pos * 4 + 3] += (float) volume / 12;
	
	FromTop = !FromTop;
}
