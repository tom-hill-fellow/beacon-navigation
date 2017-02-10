package lilium.beaconnavigation.Implementations;

import android.graphics.PointF;
import android.os.AsyncTask;
import android.util.Log;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.linear.RealVector;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import lilium.beaconnavigation.AppConfig;
import lilium.beaconnavigation.Interfaces.Beacon;
import lilium.beaconnavigation.Interfaces.LeastSquaresSolver;
import lilium.beaconnavigation.Interfaces.PositionUpdater;
import lilium.beaconnavigation.Interfaces.TrilaterationFunction;
import lilium.beaconnavigation.MainActivity;

public class MultiThreadedPositionUpdater implements PositionUpdater {
    private long lastUpdate;
    private long maxUpdate;
    private Thread positionUpdate;
    private AtomicBoolean stop;
    private AtomicInteger running;

    private final long MaxSpawnWait = AppConfig.get_maximum_spawn_wait();


    public MultiThreadedPositionUpdater(final long Maximum_Update) {
        lastUpdate = System.currentTimeMillis();
        maxUpdate = Maximum_Update;
        stop = new AtomicBoolean(false);
        running = new AtomicInteger(0);

        //Start a new thread to run the encompassed method
        positionUpdate = new Thread(new Runnable(){

            //Here is the encompassed method that runs on the thread
            public void run(){
                while(! stop.get()) {
                    if (running.compareAndSet(0,1)) { // is an async position updater running on any threads?
                        if (System.currentTimeMillis() - lastUpdate > Maximum_Update) {
                            //Runs position update only on "placed" beacons
                            new PositionUpdate().execute(MainActivity.beaconKeeper.clonePlaced());
                           try{
                                Thread.sleep(MaxSpawnWait);
                            } catch (InterruptedException e){
                                Log.e("PositionThread","Interrupted Exception");
                                e.printStackTrace();
                            }
                        }
                        else{
                            running.decrementAndGet();
                        }
                    }
                }
            }

        }, "PositionThread");
    }

    public void start()
    {
        //Starts the thread process
        positionUpdate.start();
    }

    private void updatePosition(ArrayList<Beacon> beacons) {
        double[][] positions = new double[beacons.size()][2];
        double[] distances = new double[beacons.size()];
        if (beacons.size() > 1) {
            for (int i = 0; i < beacons.size(); i++) {
                positions[i][0] = beacons.get(i).getX();
                positions[i][1] = beacons.get(i).getY();
                //we want linear distances, the distance readings don't have to be accurate
                //they just need to be consistent across all beacons
                //because the trilateration function uses them as relative to each other
                distances[i] = beacons.get(i).distance();
            }
            try {
                TrilaterationFunction triFunc = new StandardTrilaterationFunction(positions,distances);
                LeastSquaresSolver solver = new NonLinearLeastSquaresSolver(triFunc, new LevenbergMarquardtOptimizer());
                RealVector vec = solver.solve();
                double[] calculatedPosition = vec.toArray();
                MainActivity.position = new PointF((float) calculatedPosition[0], (float) calculatedPosition[1]);
                lastUpdate = System.currentTimeMillis();
                try{
                    Thread.sleep(maxUpdate);
                } catch (InterruptedException e){
                    Log.e("async_position","Interrupted Exception");
                }
            } catch (TooManyEvaluationsException e) {
                // position stays the same
                Log.e("async_position", "TOO MANY CALCULATIONS");
            }
        }
    }

    private class PositionUpdate extends AsyncTask<Object,Void,Void> {

        protected Void doInBackground(Object ... args) {
            ArrayList<Beacon> beacons;
            beacons = (ArrayList<Beacon>) args[0];
            updatePosition(beacons);
            return null;
        }

        protected void onPostExecute(Void result) {
            MainActivity.map.invalidate();
            running.decrementAndGet();
        }
    }
}

