require 'pp'
require 'erb'

$initChars = %q{_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ}
$restChars = %q{_abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789}
$names = {}
def makename
	i = $names.size
	
	name = $initChars[i % $initChars.size]
	i /= $initChars.size
	while i != 0
		name += $restChars[i % $restChars.size]
		i /= $restChars.size
	end
	name
end
def replaceNames(source)
	source.gsub /\$([a-zA-Z0-9_]+)/ do
		$names[$1] = makename if not $names.has_key? $1
		$names[$1]
	end
end
	
class ErbCapture
	def write(data)
		$erbout << data
	end
end

def render(template)
	template = ("% require 'sc'\n" + template).split "\n"
	template.map! do |line|
		stripped = line.strip
		if stripped[0] == '%' then stripped
		else line
		end
	end
	template = template.join "\n"
	
	oldStdout, oldErbout, $stdout = $stdout, $erbout, ErbCapture.new
	erb = ERB.new(template, nil, '%<>', '$erbout')
	data = erb.result(binding)
	$stdout, $erbout = oldStdout, oldErbout
	replaceNames data
end

class Find
	def initialize(block)
		@include = []
		@exclude = []
		
		instance_eval &block
	end
	
	def include(files)
		files = [files] if not files.is_a? Array
		@include += files
	end
	
	def exclude(files)
		files = [files] if not files.is_a? Array
		@exclude += files
	end
	
	def find
		files = []
		@include.each do |path|
			if path =~ /\.\.\.\//
				(0...10).each do |i|
					files += Dir.glob(path.sub '.../', ('*/'*i))
				end
			elsif path =~ /\*/
				files += Dir.glob path
			else
				files += [path]
			end
		end
		
		files.map! do |file|
			if @exclude.map do |exclude|
						if file[0...exclude.size] == exclude then true
						else false
						end
					end.include? true
				nil
			else file
			end
		end
		files.compact
	end
end

def find(&block)
	Find.new(block).find
end

def cl(out, flags=[], files=[], &block)
	if block != nil
		files += find &block
	end
	
	includes = []
	files.reject! do |file|
		if File.directory? file then
			includes[includes.size] = file
			true
		else false
		end
	end
	
	references = files.map do |file|
			if file =~ /\.lib$/ then file
			else nil
			end
		end.compact
	
	files = files.map do |file|
			if references.include? file then nil
			else file
			end
		end.compact
	
	scfiles = files.map do |file|
			if file =~ /\.sc$/ then file
			else nil
			end
		end.compact
	
	files.map! do |file|
		if file =~ /\.sc$/ then
			path = file.split('/')
			outf = 'Obj/' + path[-1].sub(/\.sc/, '.c')
			
			src = File.open(file).read
			dest = File.open outf, 'w'
			dest.write render src
			dest.close
			
			ipath = path[0...path.size-1].join '/'
			includes[includes.size] = ipath if not includes.include? ipath
			outf
		else file
		end
	end
	
	includes.map! { |path| '/I' + path }
	
	file out => (scfiles + files) do
		sh 'cl', '/TP', '/O1', "/Fe#{out}", *includes, *files, *flags, '/link', '/MACHINE:X86', '/SUBSYSTEM:WINDOWS', *references
	end
	Rake::Task[out].invoke
	
	files.each do |file|
		file.sub! /\.c$/, '.obj'
		begin
			File.unlink file.split('/')[-1]
		rescue
		end
	end
end

task :default => [:week1, :week1improved, :week2]

task :obj do
	Dir::mkdir 'Obj' if not FileTest::directory? 'Obj'
end

task :week1 => [:obj] do
	cl 'Obj/Week1.exe' do
		include [
				'C:\aaa\fmodapi375win\api\inc', 
				'C:\Program Files (x86)\Microsoft SDKs\Windows\v7.0A\Include', 
			]
		
		include %w{user32.lib opengl32.lib glu32.lib gdi32.lib}
		include 'C:\aaa\fmodapi375win\api\lib\fmodvc.lib'
		
		include 'Week1/*.c'
	end
end

task :week1improved => [:obj] do
	cl 'Obj/Week1Improved.exe' do
		include [
				'C:\aaa\fmodapi375win\api\inc', 
				'C:\Program Files (x86)\Microsoft SDKs\Windows\v7.0A\Include', 
			]
		
		include %w{user32.lib opengl32.lib glu32.lib gdi32.lib}
		include 'C:\aaa\fmodapi375win\api\lib\fmodvc.lib'
		
		include 'Week1Improved/*.c'
	end
end

task :week2 => [:obj] do
	cl 'Obj/Week2Uncomp.exe' do
		include [
				'C:\aaa\bassmod20\c', 
				'C:\Program Files (x86)\Microsoft SDKs\Windows\v7.0A\Include', 
				'C:\aaa\glew-1.5.3\include', 
			]
		
		include %w{user32.lib opengl32.lib glu32.lib gdi32.lib}
		include 'C:\aaa\glew-1.5.3\lib\glew32.lib'
		include 'C:\aaa\bassmod20\c\BASSMOD.lib'
		
		include 'Week2/.../*.sc'
		include 'Sidt/.../*.sc'
	end
	
	sh 'del', 'Obj\\Week2.exe'
	sh 'c:\AAA\upx304w\upx.exe', '-oObj/Week2.exe', '--no-reloc', '--strip-relocs=1', '--best', 'Obj/Week2Uncomp.exe'
	#sh 'c:\AAA\kkrunchy\kkrunchy_k7.exe', '--best', '--out', 'Obj/Week2.exe', 'Obj/Week2Uncomp.exe'
end
