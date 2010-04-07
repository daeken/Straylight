% import 'Week2/Week2.sh'

/*
void createNoise(GLuint texId, int size) {
	unsigned char *data;
	int dsize = size * size;
	data = (unsigned char *) malloc(dsize);
	data += dsize;
	
	for(int i = 0; i < dsize; ++i)
		*(--data) = rand() & 0xFF;
	
	glBindTexture(GL_TEXTURE_2D, texId);
	glTexImage2D(GL_TEXTURE_2D, 0, 1, size, size, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	free(data);
}*/

void createAluminum(GLuint texId, int size) {
	unsigned char *data, *noise;
	int dsize = size * size, length = size >> 2, lengthbase = log2(size >> 2);
	noise = (unsigned char *) malloc(dsize);
	noise += dsize;
	data = (unsigned char *) malloc(dsize * 3);
	
	for(int i = 0; i < dsize; ++i)
		*(--noise) = rand() & 0xFF;
	
	for(int y = 0, off = 0; y < size; ++y) {
		for(int x = 0; x < size; ++x, off += 3) {
			int val = 0;
			for(int i = 0; i < length; ++i)
				if(i & 15 == 0)
					val += noise[(y + 1) * size + ((x + i) % size)];
				else
					val += noise[y * size + ((x + i) % size)];
			val >>= lengthbase;
			data[off] = val;
			data[off+1] = val;
			data[off+2] = (val + (val >> 4)) & 0xFF;
		}
	}
	
	glBindTexture(GL_TEXTURE_2D, texId);
	glTexImage2D(GL_TEXTURE_2D, 0, 3, size, size, 0, GL_RGB, GL_UNSIGNED_BYTE, data);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	free(data);
}

void createMunchingSquares(GLuint texId, int size) {
	unsigned char *data = (unsigned char *) malloc(size * size);
	
	for(int y = 0, off = 0; y < size; ++y)
		for(int x = 0; x < size; ++x, ++off)
			data[off] = (((x ^ y) & 0xFF) ^ 0xF) >> 2;
	
	glBindTexture(GL_TEXTURE_2D, texId);
	glTexImage2D(GL_TEXTURE_2D, 0, 1, size, size, 0, GL_LUMINANCE, GL_UNSIGNED_BYTE, data);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
	free(data);
}

void gentextures() {
	glGenTextures(sizeof(Textures_s) / sizeof(GLuint), (GLuint *) &Textures);
	
	createAluminum(Textures.aluminum, 64);
	createMunchingSquares(Textures.wall, 1024);
}
