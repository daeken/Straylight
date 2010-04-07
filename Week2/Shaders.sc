% import 'Week2/Week2.sh'

void genshaders() {
	GLuint loc;
	GLint status;
	<%
		shaders = {
				:wave => ['Week2/Wave.sfs'], 
				:spheres => ['Week2/Spheres.svs', 'Week2/GenericLit.sfs'], 
				:laser => ['Week2/Laser.sfs'], 
			}
		
		shaders.each do |name, fns|
			puts '{'
			puts 'GLuint ' + (0...fns.size).map { |i| "shader_#{i}" }.join(',') + ';'
			(0...fns.size).each do |i|
				source = importstring fns[i]
				source = condenseShader source
				$stderr.puts source
				type = 
					if(fns[i] =~ /\.sfs$/)
						:GL_FRAGMENT_SHADER
					else :GL_VERTEX_SHADER
					end;
				puts %Q{
						{
							GLint status;
							const char source_[] = #{source.to_carray};
							char *source = (char *) malloc(sizeof(source_));
							memcpy(source, source_, sizeof(source_));
							shader_#{i} = glCreateShader(#{type});
							glShaderSource(shader_#{i}, 1, (const char **) &source, NULL);
							glCompileShader(shader_#{i});
							glGetShaderiv(shader_#{i}, GL_COMPILE_STATUS, &status);
							if(status != GL_TRUE) {
#ifdef DEBUG
								char buf[65536];
								int len;
								glGetShaderInfoLog(shader_#{i}, 65535, &len, buf);
								printf("Shader #{fns[i]} compilation failed: %s", buf);
#else
								exit(-1);
#endif
							}
						}
					}
			end
			puts "Shaders.#{name} = glCreateProgram();"
			(0...fns.size).each do |i|
				puts "glAttachShader(Shaders.#{name}, shader_#{i});"
			end
			puts "glLinkProgram(Shaders.#{name});"
			puts %Q{
				glGetProgramiv(Shaders.#{name}, GL_LINK_STATUS, &status);
				if(status != GL_TRUE) {
#ifdef DEBUG
					char buf[65536];
					int len;
					glGetProgramInfoLog(Shaders.#{name}, 65535, &len, buf);
					printf("Program #{name} compilation failed: %s", buf);
#else
					exit(-1);
#endif
				}
			}
			puts '}'
		end
	%>
	
	//loc = glGetUniformLocation(Shaders.aluminum, "noise");
	//glUniform1i(loc, Textures.aluminumNoise);
	
	glUseProgram(Shaders.wave);
	loc = glGetUniformLocation(Shaders.wave, "$grid1");
	glUniform1i(loc, 0);
	loc = glGetUniformLocation(Shaders.wave, "$grid2");
	glUniform1i(loc, 1);
	loc = glGetUniformLocation(Shaders.wave, "$add");
	glUniform1i(loc, 2);
	validate(Shaders.wave)
	
	glUseProgram(Shaders.spheres);
	loc = glGetUniformLocation(Shaders.spheres, "$grid");
	glUniform1i(loc, 0);
	glUseProgram(0);
	
	<%
		
	%>
}
