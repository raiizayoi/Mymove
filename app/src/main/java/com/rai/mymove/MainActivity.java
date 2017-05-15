package com.rai.mymove;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Wrapper activity demonstrating the use of the new
 * {@link SensorEvent#values rotation vector sensor}
 * ({@link Sensor#TYPE_ROTATION_VECTOR TYPE_ROTATION_VECTOR}).
 *
 * @see Sensor
 * @see SensorEvent
 * @see SensorManager
 *
 */
public class MainActivity extends Activity {
    private GLSurfaceView mGLSurfaceView;
    private SensorManager mSensorManager;
    private MyRenderer mRenderer;
    private TextView tv1,tv2,tv3;
    private LinearLayout lout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Get an instance of the SensorManager
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        // Create our Preview view and set it as the content of our
        // Activity
        mRenderer = new MyRenderer();
        mGLSurfaceView = new GLSurfaceView(this);
        mGLSurfaceView.setRenderer(mRenderer);

        lout = new LinearLayout(this);
        tv1 = new TextView(this);
        tv2 = new TextView(this);
        tv3 = new TextView(this);
        lout.setOrientation(LinearLayout.VERTICAL);
        lout.addView(tv1);
        lout.addView(tv2);
        lout.addView(tv3);
        lout.addView(mGLSurfaceView);

        setContentView(lout);


    }

    @Override
    protected void onResume() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onResume();
        mRenderer.start();
        mGLSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        // Ideally a game should implement onResume() and onPause()
        // to take appropriate action when the activity looses focus
        super.onPause();
        mRenderer.stop();
        mGLSurfaceView.onPause();
    }


    class MyRenderer implements GLSurfaceView.Renderer, SensorEventListener {
        private Cube mCube;
        private Sensor mRotationVectorSensor;
        private final float[] mRotationMatrix = new float[16];

        public MyRenderer() {
            // find the rotation-vector sensor
            mRotationVectorSensor = mSensorManager.getDefaultSensor(
                    Sensor.TYPE_ROTATION_VECTOR);
            mRotationVectorSensor = mSensorManager.getDefaultSensor(
                    Sensor.TYPE_LINEAR_ACCELERATION);

            mCube = new Cube();
            // initialize the rotation matrix to identity,and it's 4x4 row-major matrix
            /*
                /  M[ 0]   M[ 1]   M[ 2]   M[ 3]  \
                |  M[ 4]   M[ 5]   M[ 6]   M[ 7]  |
                |  M[ 8]   M[ 9]   M[10]   M[11]  |
                \  M[12]   M[13]   M[14]   M[15]  /
            */
            mRotationMatrix[0] = 1;
            mRotationMatrix[5] = 1;
            mRotationMatrix[10] = 1;
            mRotationMatrix[15] = 1;
        }

        public void start() {
            // enable our sensor when the activity is resumed, ask for
            // 10 ms updates.
            mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
        }

        public void stop() {
            // make sure to turn our sensor off when the activity is paused
            mSensorManager.unregisterListener(this);
        }

        public double[] computeDeviceOrientationFromRotationMatrix(float[] R, double[] values) {
        /*
         * 3x3 (length=9) case:
         *   /  R[ 0]   R[ 1]   R[ 2]  \
         *   |  R[ 3]   R[ 4]   R[ 5]  |
         *   \  R[ 6]   R[ 7]   R[ 8]  /
         *
         */
            if (R.length != 9)
                return values;

            if (R[8] > 0) {  // cos(beta) > 0
                values[0] = Math.atan2(-R[1], R[4]);
                values[1] = Math.asin(R[7]);           // beta (-pi/2, pi/2)
                values[2] = Math.atan2(-R[6], R[8]);   // gamma (-pi/2, pi/2)
            } else if (R[8] < 0) {  // cos(beta) < 0
                values[0] = Math.atan2(R[1], -R[4]);
                values[1] = -Math.asin(R[7]);
                values[1] += (values[1] >= 0) ? -Math.PI : Math.PI; // beta [-pi,-pi/2) U (pi/2,pi)
                values[2] = Math.atan2(R[6], -R[8]);   // gamma (-pi/2, pi/2)
            } else { // R[8] == 0
                if (R[6] > 0) {  // cos(gamma) == 0, cos(beta) > 0
                    values[0] = Math.atan2(-R[1], R[4]);
                    values[1] = Math.asin(R[7]);       // beta [-pi/2, pi/2]
                    values[2] = -Math.PI / 2;          // gamma = -pi/2
                } else if (R[6] < 0) { // cos(gamma) == 0, cos(beta) < 0
                    values[0] = Math.atan2(R[1], -R[4]);
                    values[1] = -Math.asin(R[7]);
                    values[1] += (values[1] >= 0) ? -Math.PI : Math.PI; // beta [-pi,-pi/2) U (pi/2,pi)
                    values[2] = -Math.PI / 2;          // gamma = -pi/2
                } else { // R[6] == 0, cos(beta) == 0
                    // gimbal lock discontinuity
                    values[0] = Math.atan2(R[3], R[0]);
                    values[1] = (R[7] > 0) ? Math.PI / 2 : -Math.PI / 2;  // beta = +-pi/2
                    values[2] = 0;                                        // gamma = 0
                }
            }

            // alpha is in [-pi, pi], make sure it is in [0, 2*pi).
            if (values[0] < 0)
                values[0] += 2 * Math.PI; // alpha [0, 2*pi)

            return values;
        }

        private double[] getOrientationFromRotationVector(float[] rotationVector) {
            float[] deviceRotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(deviceRotationMatrix, rotationVector);

            double[] rotationAngles = new double[3];
            computeDeviceOrientationFromRotationMatrix(deviceRotationMatrix, rotationAngles);

            return rotationAngles;
        }



        public void onSensorChanged(SensorEvent event) {
            // we received a sensor event. it is a good practice to check
            // that we received the proper event

            double[] angles;
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // convert the rotation-vector to a 4x4 matrix. the matrix
                // is interpreted by Open GL as the inverse of the
                // rotation-vector, which is what we want.
                SensorManager.getRotationMatrixFromVector(
                        mRotationMatrix , event.values);
                angles = getOrientationFromRotationVector(event.values);


                tv1.setText(String.valueOf(Math.toDegrees(angles[0])));
                tv2.setText(String.valueOf(Math.toDegrees(angles[1])));
                tv3.setText(String.valueOf(Math.toDegrees(angles[2])));
            }
            if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            }

        }

        public void onDrawFrame(GL10 gl) {
            // clear screen
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

            // set-up modelview matrix
            gl.glMatrixMode(GL10.GL_MODELVIEW);
            gl.glLoadIdentity();
            gl.glTranslatef(0, 0, -3.0f);
            gl.glMultMatrixf(mRotationMatrix, 0);

            // draw our object
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL10.GL_COLOR_ARRAY);

            mCube.draw(gl);
        }

        public void onSurfaceChanged(GL10 gl, int width, int height) {
            // set view-port
            gl.glViewport(0, 0, width, height);
            // set projection matrix
            float ratio = (float) width / height;
            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
        }

        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // dither is enabled by default, we don't need it
            gl.glDisable(GL10.GL_DITHER);
            // clear screen in white
            gl.glClearColor(1,1,1,1);
        }

        class Cube {
            // initialize our cube
            private FloatBuffer mVertexBuffer;
            private FloatBuffer mColorBuffer;
            private ByteBuffer  mIndexBuffer;

            public Cube() {
                final float vertices[] = {
                        -1, -1, -1,		 1, -1, -1,
                        1,  1, -1,	    -1,  1, -1,
                        -1, -1,  1,      1, -1,  1,
                        1,  1,  1,     -1,  1,  1,
                };

                final float colors[] = {
                        0,  0,  0,  1,  1,  0,  0,  1,
                        1,  1,  0,  1,  0,  1,  0,  1,
                        0,  0,  1,  1,  1,  0,  1,  1,
                        1,  1,  1,  1,  0,  1,  1,  1,
                };

                final byte indices[] = {
                        0, 4, 5,    0, 5, 1,
                        1, 5, 6,    1, 6, 2,
                        2, 6, 7,    2, 7, 3,
                        3, 7, 4,    3, 4, 0,
                        4, 7, 6,    4, 6, 5,
                        3, 0, 1,    3, 1, 2
                };

                ByteBuffer vbb = ByteBuffer.allocateDirect(vertices.length*4);
                vbb.order(ByteOrder.nativeOrder());
                mVertexBuffer = vbb.asFloatBuffer();
                mVertexBuffer.put(vertices);
                mVertexBuffer.position(0);

                ByteBuffer cbb = ByteBuffer.allocateDirect(colors.length*4);
                cbb.order(ByteOrder.nativeOrder());
                mColorBuffer = cbb.asFloatBuffer();
                mColorBuffer.put(colors);
                mColorBuffer.position(0);

                mIndexBuffer = ByteBuffer.allocateDirect(indices.length);
                mIndexBuffer.put(indices);
                mIndexBuffer.position(0);
            }

            public void draw(GL10 gl) {
                gl.glEnable(GL10.GL_CULL_FACE);
                gl.glFrontFace(GL10.GL_CW);
                gl.glShadeModel(GL10.GL_SMOOTH);
                gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
                gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);
                gl.glDrawElements(GL10.GL_TRIANGLES, 36, GL10.GL_UNSIGNED_BYTE, mIndexBuffer);
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }
}