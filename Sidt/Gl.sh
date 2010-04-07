#include <gl\glew.h>
#include <gl\gl.h>
#include <gl\glu.h>

<%
	class Light
		def initialize(name, &block)
			@name = name
			instance_eval &block
		end
		
		def position(x, y, z, w)
			vname = genname :position
			puts %Q{
					{
						GLfloat #{vname}[] = {#{x}, #{y}, #{z}, #{w}};
						glLightfv(
								#{@name}, 
								GL_POSITION, 
								#{vname}
							);
					}
				}
		end
		
		def diffuse(r, g, b, a)
			vname = genname :diffuse
			puts %Q{
					{
						GLfloat #{vname}[] = {#{r}, #{g}, #{b}, #{a}};
						glLightfv(
								#{@name}, 
								GL_DIFFUSE, 
								#{vname}
							);
					}
				}
		end
		
		def ambient(r, g, b)
			vname = genname :ambient
			puts %Q{
					{
						GLfloat #{vname}[] = {#{r}, #{g}, #{b}};
						glLightfv(
								#{@name}, 
								GL_AMBIENT, 
								#{vname}
							);
					}
				}
		end
		
		def specular(r, g, b, a)
			vname = genname :specular
			puts %Q{
					{
						GLfloat #{vname}[] = {#{r}, #{g}, #{b}, #{a}};
						glLightfv(
								#{@name}, 
								GL_SPECULAR, 
								#{vname}
							);
					}
				}
		end
		
		def enable
			puts "glEnable(#{@name});"
		end
	end
	def light(name, &block)
		Light.new name, &block
	end
	
	def condenseShader(source)
		len = -1
		while source.size != len
			len = source.size
			source = source.gsub /\/\/.*$/, ''
			source = source.gsub /\/\*.*\*\//m, ''
			source = source.gsub /[ \t\n]+/, ' '
			source = source.gsub /([a-zA-Z0-9_]) ([+\-*\/%&\^|;,.<>?:\(\)={}\[\]])/, '\1\2'
			source = source.gsub /([+\-*\/%&\^|;,.<>?:\(\)={}\[\]]) ([a-zA-Z0-9_])/, '\1\2'
			source = source.gsub /([+\-*\/%&\^|;,.<>?:\(\)={}\[\]]) ([+\-*\/%&\^|;,.<>?:\(\)={}\[\]])/, '\1\2'
			source = source.strip
		end
		source
	end
%>

inline void drawQuad(
		float x, float y, float z, 
		float w, float h, float d
	) {
	if(w == 0.0) {
		glTexCoord2f(0.0, 0.0);
		glVertex3f(x, y-h, z-d);
		glTexCoord2f(0.0, 1.0);
		glVertex3f(x, y-h, z+d);
		glTexCoord2f(1.0, 1.0);
		glVertex3f(x, y+h, z+d);
		glTexCoord2f(1.0, 0.0);
		glVertex3f(x, y+h, z-d);
	} else if(h == 0.0) {
		glTexCoord2f(0.0, 0.0);
		glVertex3f(x-w, y, z-d);
		glTexCoord2f(0.0, 1.0);
		glVertex3f(x-w, y, z+d);
		glTexCoord2f(1.0, 1.0);
		glVertex3f(x+w, y, z+d);
		glTexCoord2f(1.0, 0.0);
		glVertex3f(x+w, y, z-d);
	} else if(d == 0.0) {
		glTexCoord2f(0.0, 0.0);
		glVertex3f(x-w, y-h, z);
		glTexCoord2f(0.0, 1.0);
		glVertex3f(x+w, y-h, z);
		glTexCoord2f(1.0, 1.0);
		glVertex3f(x+w, y+h, z);
		glTexCoord2f(1.0, 0.0);
		glVertex3f(x-w, y+h, z);
	}
}

#define PI 3.14159

inline void drawCylinder(float base, float top, float height, float bx, float by, float bz, int segments) {
	float texStep = 1.0 / segments;
	float angleStep = 2 * PI * texStep;
	float delta = base - top;
	float length = sqrt(delta * delta + height * height);
	delta /= length;
	length = height / length;
	glBegin(GL_TRIANGLE_STRIP);
	for(int i = 0; i <= segments; i++) {
		float angle = angleStep * i;
		float x = cos(angle);
		float z = sin(angle);
		
		glNormal3f(
				length * x, 
				delta, 
				length * z
			);
		glTexCoord2f(
				texStep * i, 
				0.0f
			);
		glVertex3f(
				bx + x * base, 
				by, 
				bz + z * base
			);
		glTexCoord2f(
				texStep * i, 
				1.0f
			);
		glVertex3f(
				bx + x * top, 
				by + height, 
				bz + z * top
			);
	}
	glEnd();
}
