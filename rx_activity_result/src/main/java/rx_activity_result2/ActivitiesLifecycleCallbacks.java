package rx_activity_result2;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import io.reactivex.Observable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class ActivitiesLifecycleCallbacks {
    final Application application;
    volatile Activity liveActivityOrNull;
    volatile Set<Activity> stackedActivities;
    Application.ActivityLifecycleCallbacks activityLifecycleCallbacks;

    public ActivitiesLifecycleCallbacks(Application application) {
        this.application = application;
        this.stackedActivities = new HashSet<>();
        registerActivityLifeCycle();
    }

    private void registerActivityLifeCycle() {
        if (activityLifecycleCallbacks != null) application.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);

        activityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                liveActivityOrNull = activity;
                stackedActivities.add(activity);
            }

            @Override public void onActivityStarted(Activity activity) {}

            @Override public void onActivityResumed(Activity activity) {
                liveActivityOrNull = activity;
            }

            @Override public void onActivityPaused(Activity activity) {
                liveActivityOrNull = null;
            }

            @Override public void onActivityStopped(Activity activity) {}

            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

            @Override public void onActivityDestroyed(Activity activity) {
                stackedActivities.remove(activity);
            }
        };

        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    @Nullable Activity getLiveActivity() {
        return liveActivityOrNull;
    }

    @Nullable Activity findActivityOnStack(@NonNull Class clazz) {
        for (final Activity activity : stackedActivities) {
            if (activity.getClass() == clazz) {
                return activity;
            }
        }

        return null;
    }

    /**
     * Emits just one time a valid reference to the current activity
     * @return the current activity
     */
    volatile boolean emitted = false;
    Observable<Activity> getOLiveActivity() {
        emitted = false;
        return Observable.interval(50, 50, TimeUnit.MILLISECONDS)
                .map(new Function<Long, Object>() {
                    @Override public Object apply(Long aLong) throws Exception {
                        if (liveActivityOrNull == null) return 0;
                        return liveActivityOrNull;
                    }
                })
                .takeWhile(new Predicate<Object>() {
                    @Override public boolean test(Object candidate) throws Exception {
                        boolean continueEmitting = true;
                        if (emitted) continueEmitting = false;
                        if (candidate instanceof Activity) emitted = true;
                        return continueEmitting;
                    }
                })
                .filter(new Predicate<Object>() {
                    @Override public boolean test(Object candidate) throws Exception {
                        return candidate instanceof Activity;
                    }
                })
                .map(new Function<Object, Activity>() {
                    @Override public Activity apply(Object activity) throws Exception {
                        return (Activity) activity;
                    }
                });
    }

}
