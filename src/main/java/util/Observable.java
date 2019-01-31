package util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Objects of this class can be observed using <tt>Observer</tt> instances.
 * Using the update function it notifies its observers with any instance of the generic type T. <p>
 *
 * If observing a single instance suffices, use the <tt>ofInstance</tt> method to create a Observable instance which only updates using the given instance.
 */
public class Observable<T> {


	/**
	 * A flag that helps prevent bugs.
	 */
	private boolean updateInProcess = false;

	public Observable(){

	}

	private final List<Observer<T>> observers = new ArrayList<>();

	/**
	 * Adds the given observer to the list.
	 * @param observer observer which will be notified.
	 */
	public synchronized void observe(Observer<T> observer){
		if(updateInProcess) {
			throw new RuntimeException("Cannot add observers while update is in process.");
		}
		this.observers.add(observer);
	}

	/**
	 * Invoke when the state of the observerved object changes.
	 */
	public synchronized void update(T updatedInstance) {
		if(updateInProcess) {
			throw new RuntimeException("Cannot update again while update is in process.");
		}
		Iterator<Observer<T>> observerIterator = this.observers.iterator();
		updateInProcess = true;
		while(observerIterator.hasNext()) {
			Observer<T> observer = observerIterator.next();
			try{
				boolean removeObserver;
				if(observer.synchronizedNotification()){
					/*
					 * Observer wants synchronized notification.
					 */
					synchronized (observer){
						removeObserver = notificationProcess(observer, updatedInstance);
					}
				} else{
					/*
					 * Observer allows parallel notification.
					 */
					removeObserver = notificationProcess(observer, updatedInstance);
				}
				if(removeObserver){
					/*
					 * remove this observer from the list of observers.
					 */
					observerIterator.remove();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException(ex);
			}
		}
		updateInProcess = false;
	}

	/**
	 * Implements the notification process. <p>
	 * See the <tt>Observer</tt> documentation.
	 *
	 * @return true if the observer is to be removed from the observers list.
	 */
	private final boolean notificationProcess(Observer<T> observer, T t){
		if(observer.notifyCondition(t)){
			observer.notification(t);
			return observer.removeAfterNotification(t);
		}
		return false;
	}

	/**
	 * Creates an instance of InstanceObservable which only allows observing the single given instance.
	 */
	public static <T> Observable<T> ofInstance(T observedInstance) {
		return new InstanceObservable(observedInstance);
	}

	/**
	 * Observable which only allows observing a single instance.
	 * @param <T>
	 */
	static final class InstanceObservable<T> extends Observable<T> {


		private final T observedInstance;

		/**
		 * Default consturctor.
		 * @param observedObject the instance which is observed by the observers of the list.
		 */
		public InstanceObservable(T observedObject) {
			this.observedInstance = observedObject;
		}

		/**
		 * Overrides the implementation of update(T t) to prohibit using a different T than the one used to instantiate this Observable.
		 * @param t updated instance.
		 */
		public final synchronized void update(T t) {
			if(t == observedInstance || t == null){
				this.update();
			} else {
				throw new RuntimeException("Instance Observable doesn't allow the update(T t) function to be invoced with another instance  . " +
						"Instead call the update() function which internally uses the update(T t) function with the instance " +
						"which was used to create this Instance Observable.");
			}
		}

		/**
		 * Invoke when the state of the observed instance changed
		 */
		public final synchronized void update() {
			super.update(observedInstance);
		}


		/**
		 * Adds the given observer to the list.
		 * Additionaly executes the notificationProcess with the observedInstance.
		 * @param observer observer which will be notified.
		 */
		public final synchronized void observe(Observer<T> observer){
			if(!super.notificationProcess(observer, this.observedInstance)){
				/*
				 * if it doesn't want to be removed add it to the observers list:
				 */
				super.observe(observer);
			}
		}

	}
}