#include <windows.h>
#include <gl\gl.h>
#include <gl\glu.h>

#include <fmod.h>

#define ever ;;

HDC hDC;
HWND hWnd;
HINSTANCE hInstance;
HGLRC hRC;
BOOL active = TRUE;
BOOL keys[256];

GLuint sphere;

#define RADIUS 0.4

#define FULLSCREEN TRUE

#define GRID_WIDTH 50
#define GRID_HEIGHT 50
#define SHIFT 4
#define GRID_HW GRID_WIDTH / 2
#define GRID_HH GRID_HEIGHT / 2
#define SPACING 1.0
float grid[GRID_WIDTH*GRID_HEIGHT];
float grid1[GRID_WIDTH*GRID_HEIGHT];
float grid2[GRID_WIDTH*GRID_HEIGHT];

char *prevText = NULL;
int gridText[GRID_WIDTH*GRID_HEIGHT];
float textColor = 1.0;
BOOL textTransition = FALSE;

#define SQ(v) ((v) * (v))
#define WAVESPEED 0.9
#define WAVELIFE 0.1
#define GRIDSIZE 0.8
#define TIMEDELTA 0.1

#define ZOOM 50.0

#define SONG "miriel.it"

const float calc1 = (4 - ((8 * SQ(WAVESPEED) * SQ(TIMEDELTA)) / SQ(GRIDSIZE))) / ((WAVELIFE * TIMEDELTA) + 2);
const float calc2 = ((WAVELIFE * TIMEDELTA) - 2) / ((WAVELIFE * TIMEDELTA) + 2);
const float calc3 = ((2.0 * SQ(WAVESPEED) * SQ(TIMEDELTA)) / SQ(GRIDSIZE)) / ((WAVELIFE * TIMEDELTA) + 2);

FMUSIC_MODULE *song;

typedef struct Drop_s {
	int offset;
	float height;
} Drop;

#define DROPCOUNT 256
Drop drops[DROPCOUNT];
int *instrumentOffsets;

void addDrop(int offset, float scale) {
	int i;
	
	offset %= GRID_WIDTH * GRID_HEIGHT;
	
	for(i = 0; i < DROPCOUNT; ++i)
		if(drops[i].offset == -1) {
			drops[i].offset = offset;
			drops[i].height = 10.0;
			return;
		}
	
	grid[drops[0].offset] -= 1.0;
	drops[0].offset = offset;
	drops[0].height = 10.0 * (1.0 - scale);
}

void F_CALLBACKAPI instrumentCallback(FMUSIC_MODULE *mod, unsigned char param) {
	int row = instrumentOffsets[param];
	float *fft = FSOUND_DSP_GetSpectrum();
	
	int top = -1;
	float val = 0.0;
	
	for(int i = 0; i < 256; ++i) {
		if(fft[i] > val) {
			top = i;
			val = fft[i];
		}
	}
	
	if(top != -1)
		addDrop(row + top, val);
}

#include "chars.h"

void unsetText() {
	textTransition = TRUE;
}

void setText(char *text) {
	int x, y, len = strlen(text);
	int startx = GRID_HW - (len * 5 / 2);
	
	if(prevText != NULL && strcmp(prevText, text) == 0)
		return;
	prevText = text;
	
	textTransition = FALSE;
	textColor = 1.0;
	
	memset(gridText, 0, sizeof(int) * GRID_WIDTH * GRID_HEIGHT);
	
	for(y = 0; y < 4; ++y) {
		for(x = 0; x < len; ++x) {
			int pos = (y + GRID_HH - 2) * GRID_WIDTH + startx + (x * 5);
			
			gridText[pos  ] = chars[text[x] - 'A'][y * 4] ? (1 + (rand() % 45)) : 0;
			gridText[pos+1] = chars[text[x] - 'A'][y * 4 + 1] ? (1 + (rand() % 45)) : 0;
			gridText[pos+2] = chars[text[x] - 'A'][y * 4 + 2] ? (1 + (rand() % 45)) : 0;
			gridText[pos+3] = chars[text[x] - 'A'][y * 4 + 3] ? (1 + (rand() % 45)) : 0;
		}
	}
}

void init() {
	GLUquadricObj *quadric;
	int i, numInsts;
	GLfloat position[] = {0.0f, 25.0f, -ZOOM*0.9, 1.0f};
	GLfloat specular[] = {1.0f, 1.0f, 1.0f, 1.0f};
	GLfloat ambient[] = {0.4f, 0.4f, 0.4f};
	
	glClearColor(0.0, 0.0, 0.0, 1.0);
	glClearDepth(1.0);
	glDepthFunc(GL_LESS);
	glEnable(GL_DEPTH_TEST);
	glShadeModel(GL_SMOOTH);
	
	glBlendFunc(GL_SRC_ALPHA, GL_ONE);
	glEnable(GL_BLEND);
	
	glLightfv(
			GL_LIGHT0, 
			GL_POSITION, 
			position
		);
	glLightfv(
			GL_LIGHT0, 
			GL_AMBIENT,  
			ambient
		);
	glLightfv(
			GL_LIGHT0, 
			GL_SPECULAR, 
			specular
		);
	
	glEnable(GL_LIGHT0);
	glEnable(GL_LIGHTING);
	glEnable(GL_COLOR_MATERIAL);
	glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
	glMateriali(GL_FRONT, GL_SHININESS, 255);
	
	sphere = glGenLists(1);
	glNewList(sphere, GL_COMPILE);
	
	quadric = gluNewQuadric();
	gluQuadricNormals(quadric, GLU_SMOOTH);
	gluQuadricTexture(quadric, GL_FALSE);
	gluSphere(quadric, RADIUS, 32, 32);
	gluDeleteQuadric(quadric);
	
	glEndList();
	
	for(i = 0; i < DROPCOUNT; ++i)
		drops[i].offset = -1;
	
	FSOUND_Init(44100, 64, FSOUND_INIT_USEDEFAULTMIDISYNTH);
	song = FMUSIC_LoadSong(SONG);
	if(song == NULL)
		song = FMUSIC_LoadSong("Obj\\" SONG);
	FSOUND_DSP_SetActive(FSOUND_DSP_GetFFTUnit(), TRUE);
	FMUSIC_SetMasterVolume(song, 256);
	
	srand(0xDEADBEEF);
	
	numInsts = FMUSIC_GetNumInstruments(song);
	instrumentOffsets = (int *) malloc(sizeof(int) * numInsts);
	for(i = 0; i < numInsts; ++i) {
		FMUSIC_SetInstCallback(song, instrumentCallback, i);
		instrumentOffsets[i] = rand() % (GRID_HEIGHT * GRID_WIDTH);
	}
	
	FMUSIC_PlaySong(song);
	
	setText("STRAYLIGHT");
}

void resize(int width, int height) {
	glViewport(0, 0, width, height);
	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	gluPerspective(60.0, (float) width / (float) height, 0.1, 10000.0);
	glMatrixMode(GL_MODELVIEW);
}

void updateGrid() {
	int off, x, y;
	
	memcpy(grid2, grid1, sizeof(float) * GRID_HEIGHT * GRID_WIDTH);
	memcpy(grid1, grid, sizeof(float) * GRID_HEIGHT * GRID_WIDTH);
	
	off = GRID_WIDTH+1;
	for(x = 1; x < GRID_WIDTH - 1; ++x, off += 2)
		for(y = 1; y < GRID_HEIGHT - 1; ++y, ++off)
			grid[off] = 
				(calc1 * grid1[off]) + 
				(calc2 * grid2[off]) + 
				(calc3 * (
						grid1[off+1] + grid1[off-1] + grid1[off+GRID_WIDTH] + grid1[off-GRID_WIDTH]
					));
	
	x = (GRID_HEIGHT - 1) * GRID_WIDTH;
	for(y = 1; y < GRID_HEIGHT - 1; ++y) {
		off = y;
		grid[off] = 
			(calc1 * grid1[off]) + 
			(calc2 * grid2[off]) + 
			(calc3 * (
					grid1[off+1] + grid1[off-1] + grid1[off+GRID_WIDTH]
				));
		off += x;
		grid[off] = 
			(calc1 * grid1[off]) + 
			(calc2 * grid2[off]) + 
			(calc3 * (
					grid1[off+1] + grid1[off-1] + grid1[off-GRID_WIDTH]
				));
	}
	
	off = GRID_WIDTH;
	for(x = 1; x < GRID_WIDTH - 1; ++x, ++off) {
		grid[off] = 
			(calc1 * grid1[off]) + 
			(calc2 * grid2[off]) + 
			(calc3 * (
					grid1[off+1] + grid1[off-GRID_WIDTH] + grid1[off+GRID_WIDTH]
				));
		off += GRID_WIDTH-1;
		grid[off] = 
			(calc1 * grid1[off]) + 
			(calc2 * grid2[off]) + 
			(calc3 * (
					grid1[off-1] + grid1[off-GRID_WIDTH] + grid1[off+GRID_WIDTH]
				));
	}
	
	grid[0] = 
		(calc1 * grid1[0]) + 
		(calc2 * grid2[0]) + 
		(calc3 * (
				grid1[1] + grid1[GRID_WIDTH]
			));
	
	off = GRID_WIDTH-1;
	grid[off] = 
		(calc1 * grid1[off]) + 
		(calc2 * grid2[off]) + 
		(calc3 * (
				grid1[off-1] + grid1[off+GRID_WIDTH]
			));
	
	off = GRID_WIDTH * (GRID_HEIGHT - 1);
	grid[off] = 
		(calc1 * grid1[off]) + 
		(calc2 * grid2[off]) + 
		(calc3 * (
				grid1[off+1] + grid1[off-GRID_WIDTH]
			));
	
	off = GRID_WIDTH * GRID_HEIGHT - 1;
	grid[off] = 
		(calc1 * grid1[off]) + 
		(calc2 * grid2[off]) + 
		(calc3 * (
				grid1[off-1] + grid1[off-GRID_WIDTH]
			));
}

void checktime() {
	int time = FMUSIC_GetTime(song) / 1000;
	if(time > 10 && time < 20)
		unsetText();
	if(time > 20 && time < 30)
		setText("PRESENTS");
	else if(time > 30 && time < 40)
		unsetText();
	else if(time > 40 && time < 50)
		setText("WAVERIDE");
	else if(time > 50 && time < 60)
		unsetText();
	else if(time > 80 && time < 90)
		setText("NIGHTBEAT");
	else if(time > 90 && time < 100)
		unsetText();
	else if(time > 100 && time < 110)
		setText("ASD");
	else if(time > 110 && time < 120)
		unsetText();
	else if(time > 120 && time < 130)
		setText("SVATG");
	else if(time > 130 && time < 140)
		unsetText();
	else if(time > 140 && time < 150)
		setText("LATERALUS");
	else if(time > 150 && time < 160)
		unsetText();
	else if(time > 160 && time < 170)
		setText("KEWLERS");
	else if(time > 170)
		unsetText();
}

void render() {
	int i, x, y, off = 0;
	float prev = 0.0;
	
	checktime();
	
	glLoadIdentity();
	glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	
	glTranslatef(0.0, 5.0, -ZOOM);
	glRotatef(60, 1.0, 0.0, 0.0);
	
	glColor3f(0.0, 1.0, 0.0);
	for(i = 0; i < DROPCOUNT; ++i) {
		if(drops[i].offset == -1)
			continue;
		
		if(drops[i].height > 1.0) {
			int off = drops[i].offset;
			
			glPushMatrix();
			
			glTranslatef(((off % GRID_WIDTH) - GRID_HH) * SPACING, drops[i].height, (off / GRID_WIDTH - GRID_HW) * SPACING);
			glScalef(0.5, 1.1, 0.5);
			
			glCallList(sphere);
			
			glPopMatrix();
			drops[i].height -= 1.0;
		} else {
			if(grid[drops[i].offset] >= 0.1)
				grid[drops[i].offset] -= 0.5;
			else
				grid[drops[i].offset] += 0.5;
			drops[i].offset = -1;
		}
	}
	
	updateGrid();
	
	glColor3f(0.0, 0.0, 1.0);
	
	glTranslatef(GRID_HW * SPACING, 0.0, -GRID_HH * SPACING);
	for(y = 0; y < GRID_HEIGHT; ++y) {
		glTranslatef(-GRID_WIDTH * SPACING, -prev, SPACING);
		prev = 0.0;
		
		for(x = 0; x < GRID_WIDTH; ++x, ++off) {
			if(gridText[off] == 1)
				glColor4f(textColor, 0.0, 1.0 - textColor, 0.4 + (0.6 * textColor));
			else {
				if(gridText[off] != 0)
					--gridText[off];
				glColor4f(0.0, 0.0, 1.0, 0.4);
			}
			glTranslatef(SPACING, grid[off]-prev, 0.0);
			glCallList(sphere);
			
			prev = grid[off];
		}
	}
	
	if(textTransition && textColor > 0.0) {
		textColor -= 0.05;
		if(textColor < 0.0)
			textColor = 0.0;
	}
}

void killWindow() {
}

LRESULT CALLBACK WndProc(
		HWND hWnd, 
		UINT uMsg, 
		WPARAM wParam, 
		LPARAM lParam
	) {
	switch(uMsg) {
		case WM_KEYDOWN:
			keys[wParam] = TRUE;
			return 0;
		
		case WM_KEYUP:
			keys[wParam] = FALSE;
			return 0;
		
		case WM_CLOSE:
			PostQuitMessage(0);
			return 0;
		
		case WM_SIZE:
			resize(LOWORD(lParam), HIWORD(lParam));
			return 0;
	}
	
	return DefWindowProc(hWnd, uMsg, wParam, lParam);
}

BOOL createWindow() {
	GLuint PixelFormat;
	WNDCLASS wc;
	DWORD dwExStyle;
	DWORD dwStyle;
	RECT WindowRect;
	
	int width = GetSystemMetrics(SM_CXSCREEN);
	int height = GetSystemMetrics(SM_CYSCREEN);
	
	WindowRect.left=(long)0;
	WindowRect.right=(long)width;
	WindowRect.top=(long)0;
	WindowRect.bottom=(long)height;
	
	hInstance = GetModuleHandle(NULL);
	wc.style = CS_HREDRAW | CS_VREDRAW | CS_OWNDC;
	wc.lpfnWndProc = (WNDPROC) WndProc;
	wc.cbClsExtra = 0;
	wc.cbWndExtra = 0;
	wc.hInstance = hInstance;
	wc.hIcon = LoadIcon(NULL, IDI_WINLOGO);
	wc.hCursor = LoadCursor(NULL, IDC_ARROW);
	wc.hbrBackground = NULL;
	wc.lpszMenuName = NULL;
	wc.lpszClassName = "OpenGL";
	
	if(!RegisterClass(&wc)) {
		MessageBox(NULL,"Failed To Register The Window Class.","ERROR",MB_OK|MB_ICONEXCLAMATION);
		return FALSE;
	}
	
	if(FULLSCREEN) {
		DEVMODE dmScreenSettings;
		memset(&dmScreenSettings,0,sizeof(dmScreenSettings));
		dmScreenSettings.dmSize=sizeof(dmScreenSettings);
		dmScreenSettings.dmPelsWidth = width;
		dmScreenSettings.dmPelsHeight = height;
		dmScreenSettings.dmBitsPerPel = 32;
		dmScreenSettings.dmFields=DM_BITSPERPEL|DM_PELSWIDTH|DM_PELSHEIGHT;
		
		if(ChangeDisplaySettings(&dmScreenSettings,CDS_FULLSCREEN)!=DISP_CHANGE_SUCCESSFUL) {
			return FALSE;
		}
		
		dwExStyle=WS_EX_APPWINDOW;
		dwStyle=WS_POPUP;
		ShowCursor(FALSE);
	} else {
		dwExStyle=WS_EX_APPWINDOW | WS_EX_WINDOWEDGE;
		dwStyle=WS_OVERLAPPEDWINDOW;
	}
	
	AdjustWindowRectEx(&WindowRect, dwStyle, FALSE, dwExStyle);
	
	if(
		!(hWnd=CreateWindowEx(
			dwExStyle,
			"OpenGL",
			"Waveride by Straylight",
			dwStyle |
			WS_CLIPSIBLINGS |
			WS_CLIPCHILDREN,
			0, 0,
			WindowRect.right-WindowRect.left,
			WindowRect.bottom-WindowRect.top,
			NULL,
			NULL,
			hInstance,
			NULL))
		) {
		killWindow();
		return FALSE;
	}
	
	static PIXELFORMATDESCRIPTOR pfd = {
			sizeof(PIXELFORMATDESCRIPTOR),
			1,
			PFD_DRAW_TO_WINDOW |
			PFD_SUPPORT_OPENGL |
			PFD_DOUBLEBUFFER,
			PFD_TYPE_RGBA,
			32,
			0, 0, 0, 0, 0, 0,
			0,
			0,
			0,
			0, 0, 0, 0,
			16,
			0,
			0,
			PFD_MAIN_PLANE,
			0,
			0, 0, 0
		};
	
	if(!(hDC=GetDC(hWnd))) {
		killWindow();
		return FALSE;
	}
	
	if(!(PixelFormat=ChoosePixelFormat(hDC,&pfd))) {
		killWindow();
		return FALSE;
	}
	
	if(!SetPixelFormat(hDC,PixelFormat,&pfd)) {
		killWindow();
		return FALSE;
	}
	
	if(!(hRC=wglCreateContext(hDC))) {
		killWindow();
		return FALSE;
	}
	
	if(!wglMakeCurrent(hDC,hRC)) {
		killWindow();
		return FALSE;
	}
	
	ShowWindow(hWnd,SW_SHOW);
	SetForegroundWindow(hWnd);
	SetFocus(hWnd);
	resize(width, height);
	
	init();
	
	return TRUE;
}

int WINAPI WinMain(
		HINSTANCE hInstance, 
		HINSTANCE hPrevInstance, 
		LPSTR lpCmdLine, 
		int nCmdShow
	) {
	MSG msg;
	
	createWindow();
	
	for(ever) {
		if(FMUSIC_IsFinished(song))
			break;
		
		if(PeekMessage(&msg, NULL, 0, 0, PM_REMOVE)) {
			if(msg.message == WM_QUIT)
				break;
			else {
				TranslateMessage(&msg);
				DispatchMessage(&msg);
			}
		} else if(active) {
			if(keys[VK_ESCAPE])
				break;
			
			render();
			SwapBuffers(hDC);
		}
	}
	
	killWindow();
	
	return msg.wParam;
}
