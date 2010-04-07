$nameIndex = 0

class String
	def to_cstr
		#cstr = self.split('').map { |x| '\\x' + x.ord.to_s(16) }.join ''
		#'"' + cstr + '"'
		inspect.strip
	end
	def to_carray
		elems = self.split('').map { |x| x.ord.to_s }
		"{#{elems.join ', '}, 0}"
	end
end

class Array
	def params
		self.map { |x| x.inspect }.join ', '
	end
	
	def push(x)
		self[self.size] = x
		self
	end
end

def import(fn)
	File.open(fn) do |fp|
		puts render fp.read
	end
end

def importstring(fn)
	File.open(fn) do |fp|
		render(fp.read)
	end
end

def importcstring(fn)
	File.open(fn) do |fp|
		render(fp.read).to_cstr
	end
end

def importcarray(fn)
	File.open(fn) do |fp|
		render(fp.read).to_carray
	end
end

def genname(base)
	$nameIndex += 1
	"#{base}_#{$nameIndex}"
end

$classes = {}
class SClass
	def initialize(name, &block)
		$classes[name] = self
		
		@name = name
		@slots = []
		@methods = {}
		
		instance_eval &block
		
		puts "typedef struct #{name}_s {"
		@slots.each do |slot| puts "\t#{slot};" end
		puts "} *#{name};"
		
		@methods.each do |mname, value|
			ret, types = value
			puts"#{ret} #{name}_#{mname}(#{types});"
		end
	end
	
	def slot(decl)
		@slots.push decl
	end
	
	def ctor(types='')
		method @name, :ctor, types
	end
	
	def ctorImpl(&block)
		ret, types = @methods[:ctor]
		puts "#{ret} #{@name}_ctor(#{types}) {"
		puts "\t#{@name} self = (#{@name}) malloc(sizeof(#{@name}_s));"
		block.call
		puts "\treturn self;"
		puts "}"
	end
	
	def method(ret, name, types='')
		if name != :ctor
			types = 
				if types == '' then "#{@name} self"
				else "#{@name} self, #{types}"
				end
		end
		@methods[name] = [ret, types]
	end
	
	def methodImpl(name, &block)
		ret, types = @methods[name]
		puts "#{ret} #{@name}_#{name}(#{types}) {"
		block.call
		puts "}"
	end
end
def sclass(name, &block)
	SClass.new name, &block
end

class Symbol
	def ctor(&block)
		$classes[self].ctorImpl &block
	end
	
	def method(name, &block)
		$classes[self].methodImpl name, &block
	end
end

$defines = []
def define(decl)
	sdecl = decl.join ' '
	if $defines.include? decl[-1]
		puts "extern #{sdecl};"
	else
		$defines.push decl[-1]
		puts "#{sdecl};"
	end
end
