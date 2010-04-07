% import 'Sidt/Sidt.sh'

#include <stdio.h>
#include <io.h>
#include <fcntl.h>

#define ever ;;
#define FULLSCREEN <%= if $fullscreen then 'TRUE' else 'FALSE' end %>

HDC hDC;
HWND hWnd;
HINSTANCE hInstance;
HGLRC hRC;
BOOL active = TRUE;
BOOL keys[256];

% if $defaultResize == true
void resize(int width, int height) {
	glViewport(0, 0, width, height);
	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	gluPerspective(60.0, (float) width / (float) height, 0.1, 10000.0);
	glMatrixMode(GL_MODELVIEW);
}
% end

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
	
	% if $fullscreen
		int width = GetSystemMetrics(SM_CXSCREEN);
		int height = GetSystemMetrics(SM_CYSCREEN);
	% else
		int width = <%= $windowSize[0] %>;
		int height = <%= $windowSize[1] %>;
	% end
	
#ifdef DEBUG
	AllocConsole();
	{
		HANDLE handle_out = GetStdHandle(STD_OUTPUT_HANDLE);
		int hCrt = _open_osfhandle((long) handle_out, _O_TEXT);
		FILE* hf_out = _fdopen(hCrt, "w");
		setvbuf(hf_out, NULL, _IONBF, 1);
		*stdout = *hf_out;
		
		HANDLE handle_in = GetStdHandle(STD_INPUT_HANDLE);
		hCrt = _open_osfhandle((long) handle_in, _O_TEXT);
		FILE* hf_in = _fdopen(hCrt, "r");
		setvbuf(hf_in, NULL, _IONBF, 128);
		*stdin = *hf_in;
	}
#endif
	
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
			<%= $demotitle.to_cstr %>,
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
	
	glewInit();
	init();
	
	return TRUE;
}

BOOL done = FALSE;
int endFrames = 100;
int WINAPI WinMain(
		HINSTANCE hInstance, 
		HINSTANCE hPrevInstance, 
		LPSTR lpCmdLine, 
		int nCmdShow
	) {
	MSG msg;
	
	createWindow();
	
	while(!done || endFrames-- != 0) {
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
	
	quit();
	killWindow();
	
	return msg.wParam;
}

void finish() {
	done = TRUE;
}
