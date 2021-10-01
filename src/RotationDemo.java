
	import com.jogamp.newt.event.*;
	import com.jogamp.newt.opengl.GLWindow;
	import com.jogamp.opengl.*;
	import com.jogamp.opengl.math.FloatUtil;
	import com.jogamp.opengl.util.Animator;
	import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
	import com.jogamp.opengl.util.glsl.ShaderProgram;

	import java.io.File;
	import java.io.FileNotFoundException;
	import java.nio.ByteBuffer;
	import java.nio.FloatBuffer;
	import java.nio.IntBuffer;
	import java.nio.ShortBuffer;
	import java.util.Scanner;

	import static com.jogamp.opengl.GL.*;
	import static com.jogamp.opengl.GL.GL_FLOAT;
	import static com.jogamp.opengl.GL.GL_TRIANGLES;
	import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
	import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_HIGH;
	import static com.jogamp.opengl.GL2ES2.GL_DEBUG_SEVERITY_MEDIUM;
	import static com.jogamp.opengl.GL2ES3.*;
	import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
	import static com.jogamp.opengl.GL4.GL_MAP_COHERENT_BIT;
	import static com.jogamp.opengl.GL4.GL_MAP_PERSISTENT_BIT;

/**
 * 
 */

/**
 * @author Jerry Heuring
 *
 * 9/20/2021 : Updated to do 3 views and rotation using arrow keys.  
 * 9/20/2021 : Fixed error in call to glDrawArrays() -- number of vertices was incorrect.
 * 
 */
public class RotationDemo {
    private interface Buffer {

        int VERTEX = 0;
        int ELEMENT = 1;
        int GLOBAL_MATRICES = 2;
        int MODEL_MATRIX = 3;
        int MAX = 4;
    }	
    /**
	 * Created by GBarbieri on 16.03.2017.
	 * 
	 * Program heavily modified by Jerry Heuring in September 2021.  
	 * Most modifications stripped out code that was not yet needed
	 * reorganized the remaining code to more closely align with the C/C++
	 * version of the initial program. 
	 */
	public class HelloTriangleSimple implements GLEventListener, KeyListener {

	    private GLWindow window;
	    private Animator animator;
	    private boolean shouldRotate = true;

	    public void main(String[] args) {
	        new HelloTriangleSimple().setup();
	    }

		private float[] vertices = null;
		private float[] colors = null;


	    private final IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX);
	    private final IntBuffer vertexArrayName = GLBuffers.newDirectIntBuffer(1);
        private Program program;
		private final PMVMatrix rotationMatrix = new PMVMatrix();
	    private final PMVMatrix viewMatrix = new PMVMatrix();
	    private final PMVMatrix projectionMatrix = new PMVMatrix();

	    private void setup() {

	        GLProfile glProfile = GLProfile.get(GLProfile.GL4);
	        GLCapabilities glCapabilities = new GLCapabilities(glProfile);

	        // file should be in the src directory
			// e.g. "src/diamond.dat"
	        readVerticesAndColors("diamond.dat");

	        window = GLWindow.create(glCapabilities);

	        window.setTitle("Rotation Demo");
	        window.setSize(600, 600);

	        window.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
	        window.setVisible(true);

	        window.addGLEventListener(this);
	        window.addKeyListener(this);

	        animator = new Animator(window);
	        animator.start();

	        window.addWindowListener(new WindowAdapter() {
	            @Override
	            public void windowDestroyed(WindowEvent e) {
	                animator.stop();
	                System.exit(1);
	            }
	        });
	    }

	    private void readVerticesAndColors(String filename) {
	    	try (Scanner file = new Scanner(new File("src/" + filename))) {
	    		// Grab the number of vertices from the top of the data file, and setup the arrays
	    		int numVert = file.nextInt();
	    		vertices = new float[numVert * 4];
	    		colors = new float[numVert * 4];

	    		// Read vertices in one line at a time
	    		for (int i = 0; i < numVert; i++) {
	    			vertices[4 * i] = file.nextFloat();
	    			vertices[(4 * i) + 1] = file.nextFloat();
					vertices[(4 * i) + 2] = file.nextFloat();
					vertices[(4 * i) + 3] = file.nextFloat();
				}

	    		// Read colors in one line at a time
	    		for (int i = 0; i < numVert; i++) {
					colors[4 * i] = file.nextFloat();
					colors[(4 * i) + 1] = file.nextFloat();
					colors[(4 * i) + 2] = file.nextFloat();
					colors[(4 * i) + 3] = file.nextFloat();
				}

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

	    @Override
	    public void init(GLAutoDrawable drawable) {

	        GL4 gl = drawable.getGL().getGL4();

	        initDebug(gl);
	        program = new Program(gl, "src/", "passthrough", "passthrough");
	        rotationMatrix.glLoadIdentity();
	        viewMatrix.glLoadIdentity();
	        projectionMatrix.glLoadIdentity();
	        // sets up orthographic projection from (-1, -100, -1) to (1, 100, 1) for the program
	        projectionMatrix.glOrthof(-1.0f, 1.0f, -1.0f, 1.0f, -100f, 100f);
	        buildObjects(gl);

	        gl.glEnable(GL_DEPTH_TEST);
			long start = System.currentTimeMillis();
	    }

	    private void initDebug(GL4 gl) {

	        window.getContext().addGLDebugListener(System.out::println);
	        /*
	         * sets up medium and high severity error messages to be printed.
	         */
	        gl.glDebugMessageControl(
	                GL_DONT_CARE,  GL_DONT_CARE, GL_DONT_CARE,
	                0, null, false);

	        gl.glDebugMessageControl(
	                GL_DONT_CARE,  GL_DONT_CARE, GL_DEBUG_SEVERITY_HIGH,
	                0, null, true);

	        gl.glDebugMessageControl(
	                GL_DONT_CARE,  GL_DONT_CARE, GL_DEBUG_SEVERITY_MEDIUM,
	                0, null, true);
	    }

	    private void buildObjects(GL4 gl) {

	        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertices);
	        FloatBuffer colorBuffer  = GLBuffers.newDirectFloatBuffer(colors);

	        gl.glGenVertexArrays(1, vertexArrayName);
	        gl.glBindVertexArray(vertexArrayName.get(0));
	        gl.glGenBuffers(Buffer.MAX, bufferName);
	        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
	        gl.glBufferData(GL_ARRAY_BUFFER,  (vertexBuffer.capacity()+colorBuffer.capacity()) * 4L, null, GL_STATIC_DRAW);
	        gl.glBufferSubData(GL_ARRAY_BUFFER,0L,vertexBuffer.capacity() * 4L, vertexBuffer);
	        gl.glBufferSubData(GL_ARRAY_BUFFER, vertexBuffer.capacity() * 4L, colorBuffer.capacity()* 4L, colorBuffer);

	        int vPosition = gl.glGetAttribLocation(program.name, "vPosition");
	        int vColor = gl.glGetAttribLocation(program.name, "vColor");
	        gl.glEnableVertexAttribArray(vPosition);
	        gl.glVertexAttribPointer(vPosition, 4, GL_FLOAT, false, 0, 0);
	        gl.glEnableVertexAttribArray(vColor);
	        gl.glVertexAttribPointer(vColor, 4, GL_FLOAT, false, 0, vertexBuffer.capacity() * 4L);
	    }


	    @Override
	    /*
	     * Display the object.  One issue with this is that it has the number 
	     * of triangles hardcoded at the moment -- hopefully I will fix this
	     * so that it comes from the buffer or some other reasonable object.
	     * (non-Javadoc)
	     * @see com.jogamp.opengl.GLEventListener#display(com.jogamp.opengl.GLAutoDrawable)
	     */
	    public void display(GLAutoDrawable drawable) {

	        GL4 gl = drawable.getGL().getGL4();

	        gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
	        gl.glUseProgram(program.name);
	        gl.glBindVertexArray(vertexArrayName.get(0));
	        gl.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
	        int modelMatrixLocation = gl.glGetUniformLocation(program.name, "modelingMatrix");
	        gl.glUniformMatrix4fv(modelMatrixLocation, 1, false, rotationMatrix.glGetMatrixf());
	        int viewMatrixLocation = gl.glGetUniformLocation(program.name, "viewingMatrix");
	        gl.glUniformMatrix4fv(viewMatrixLocation,  1,  false,  viewMatrix.glGetMatrixf());
	        int projectionMatrixLocation = gl.glGetUniformLocation(program.name, "projectionMatrix");
	        gl.glUniformMatrix4fv(projectionMatrixLocation, 1, false, projectionMatrix.glGetMatrixf());
	        gl.glDrawArrays(GL_TRIANGLES, 0, vertices.length/4);

			if (shouldRotate) {
				rotationMatrix.glRotatef(3.0f, 0.0f, 1.0f, 0.0f);
			}
		}

	    @Override
	    /*
	     * handles window reshapes -- it should affect the size of the
	     * view as well so that things remain square but since we haven't
	     * gotten to projections yet it does not.
	     * @see com.jogamp.opengl.GLEventListener#reshape(com.jogamp.opengl.GLAutoDrawable, int, int, int, int)
	     */
	    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

	        GL4 gl = drawable.getGL().getGL4();
	        gl.glViewport(x, y, width, height);
	    }

	    @Override
/*
 * This method disposes of resources cleaning up
 * at the end.  This wasn't happening in the C/C++
 * version but would be a good idea.
 */ 
  	    public void dispose(GLAutoDrawable drawable) {
	        GL4 gl = drawable.getGL().getGL4();

	        gl.glDeleteProgram(program.name);
	        gl.glDeleteVertexArrays(1, vertexArrayName);
	        gl.glDeleteBuffers(Buffer.MAX, bufferName);
	    }

	    @Override
	    /*
	     * Keypress callback for java -- handle a keypress
	     * (non-Javadoc)
	     * @see com.jogamp.newt.event.KeyListener#keyPressed(com.jogamp.newt.event.KeyEvent)
	     */
	    public void keyPressed(KeyEvent e) {
	        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
	            new Thread(() -> window.destroy()).start();
	        } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
	        	rotationMatrix.glRotatef(10.0f, 0.0f, 1.0f, 0.0f);
	        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
	        	rotationMatrix.glRotatef(-10.0f, 0.0f, 1.0f, 0.0f);	
	        } else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_W) {
				rotationMatrix.glRotatef(10.0f, 1.0f, 0.0f, 0.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_S) {
				rotationMatrix.glRotatef(-10.0f, 1.0f, 0.0f, 0.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_X) {
	        	// look down x-axis towards origin

	        	viewMatrix.glLoadIdentity();
	        	// eye position (1, 0, 0), look at (0, 0, 0), up direction (0, 1, 0)
	        	viewMatrix.gluLookAt(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
	        } else if (e.getKeyCode() == KeyEvent.VK_Y) {
	        	// look down y-axis towards origin

	        	viewMatrix.glLoadIdentity();
	        	// eye position (0, 3, 0), look at (0, 0, 0), up direction (1, 0, 0)
	        	viewMatrix.gluLookAt(0.0f, 3.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_Z) {
	        	// look down z-axis towards origin

				viewMatrix.glLoadIdentity();
				// eye position (0, 0, 1), look at (0, 0, 0), up direction (0, 1, 0)
				viewMatrix.gluLookAt(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);
			} else if (e.getKeyCode() == KeyEvent.VK_P) {
	        	// Pause rotation
				shouldRotate = false;
			} else if (e.getKeyCode() == KeyEvent.VK_R) {
	        	// Resume rotation
				shouldRotate = true;
			}
	    }

	    @Override
	    public void keyReleased(KeyEvent e) {
	    }

	    	/*
	    	 * private class to handle building the shader
	    	 * program from filenames.  This one is different
	    	 * from the C/C++ one in that it does not take the
	    	 * complete path.  It has a path and a file name and
	    	 * then insists on the extensions .vert, .frag.
	    	 * 
	    	 * I think we will rewrite this one to do a few other
	    	 * things before the class is over.  Right now it works.
	    	 */
	    private class Program {

	        public int name = 0;

	        public Program(GL4 gl, String root, String vertex, String fragment) {

	            ShaderCode vertShader = ShaderCode.create(gl, GL_VERTEX_SHADER, this.getClass(), root, null, vertex,
	                    "vert", null, true);
	            ShaderCode fragShader = ShaderCode.create(gl, GL_FRAGMENT_SHADER, this.getClass(), root, null, fragment,
	                    "frag", null, true);

	            ShaderProgram shaderProgram = new ShaderProgram();

	            shaderProgram.add(vertShader);
	            shaderProgram.add(fragShader);

	            shaderProgram.init(gl);

	            name = shaderProgram.program();

	            shaderProgram.link(gl, System.err);
	        }
	    }

	    /*
	     * Class to set up debug output from OpenGL.
	     * Again, I haven't done this in the C/C++ version
	     * but it would be a good idea.  
	     */
	    private class GlDebugOutput implements GLDebugListener {

	        private int source = 0;
	        private int type = 0;
	        private int id = 0;
	        private int severity = 0;
	        private final int length = 0;
	        private String message = null;

			public GlDebugOutput() {
	        }

	        public GlDebugOutput(int source, int type, int severity) {
	            this.source = source;
	            this.type = type;
	            this.severity = severity;
	            this.message = null;
	            this.id = -1;
	        }

	        public GlDebugOutput(String message, int id) {
	            this.source = -1;
	            this.type = -1;
	            this.severity = -1;
	            this.message = message;
	            this.id = id;
	        }

	        @Override
	        public void messageSent(GLDebugMessage event) {

	            if (event.getDbgSeverity() == GL_DEBUG_SEVERITY_LOW || event.getDbgSeverity() == GL_DEBUG_SEVERITY_NOTIFICATION)
	                System.out.println("GlDebugOutput.messageSent(): " + event);
	            else
	                System.err.println("GlDebugOutput.messageSent(): " + event);

				boolean received = false;
				if (null != message && message.equals(event.getDbgMsg()) && id == event.getDbgId())
	                received = true;
	            else if (0 <= source && source == event.getDbgSource() && type == event.getDbgType() && severity == event.getDbgSeverity())
	                received = true;
	        }
	    }
	}
	/**
	 * Default constructor for the class does nothing in this case.
	 * It simply gives a starting point to create an instance and then 
	 * run the main program from the class.
	 */
	public RotationDemo() {
		// TODO Auto-generated constructor stub
	}

	/**
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RotationDemo myInstance = new RotationDemo();
		HelloTriangleSimple example = myInstance.new HelloTriangleSimple();
		example.main(args);
	}

}
