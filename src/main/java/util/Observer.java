package util;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implementations of this interface can be added to <tt>de.upb.sede.util.Observable</tt> objects with the same generic type.
 * When the state of the Observable object changes and <tt>notifyCondition</tt> returns true, the <tt>notification</tt> method will be invoked.<p>
 *
 * Additionally, after the notification method is called, the Observable object will check if the observer can be removed using the <tt>removeAfterNotification</tt> method.
 * The default implementation of this function returns true so each observer would only be notified once.<p>
 * Override the function <tt>removeAfterNotification</tt> to change this behaviour.<p>
 *
 * To sum it up, each time the observed instance is updated and the notify condition holds, these 3 methods are invoked in this order:<p>
 *
 * <tt>notifyCondition -> notification -> removeAfterNotification</tt><p>
 *
 * Be aware that there can be multiple threads with different Observable instances in each method at the same time.<p>
 * If synchronization is necessary, override the <tt>synchronizedNotification</tt> function and return true to enforce that all 3 function are invoked synchronously.
 */
public interface Observer<T>{

	/**
	 * Condition which must be met in order to be notified.
	 * @param t Observed instance.
	 * @return true, if the state meets the conditions for this obsersver to be notified.
	 */
	boolean notifyCondition(T t);

	/**
	 * This method is invoked when any of the observed instances change their state.
	 * The Task itself is given as an argument to the invocation.
	 *
	 * When implementing this function be aware of racing conditions.
	 * There can be multiple threads in this method at once as the notifyObservers functions in Task is usually called by workers.
	 *
	 * @param t the observerd instance whose state has changed.
	 */
	void notification(T t);

	/**
	 * If this method returns true the obsersver will be removed from the list of observers after it is notified.
	 * Each time the observer is notified the remove condition will be checked.
	 * Default implmentation is true, which means that the observer is removed after the first notification.
	 * (Override this function to prevent this from happening.)
	 *
	 * @return true if the observer is to be removed from the list of observers.
	 */
	default boolean removeAfterNotification(T t) {
		return true;
	}

	/**
	 * Defines whether or not the notifcation process should be synchronized.
	 */
	default boolean synchronizedNotification() {
		return false;
	}

	/**
	 * Same as {@link Observer#lambda(Function, Consumer)} but with t->true as the notification condition (1st argument).
	 */
	public static <T> Observer<T> alwaysNotify(Consumer<T> notification) {
		return lambda(t-> true, notification);
	}

	/**
	 * Same as {@link Observer#lambda(Function, Consumer, Function)} but with t->true as the notification condition (1st argument).
	 */
	public static <T> Observer<T> alwaysNotify(Consumer<T> notification, Function<T, Boolean> removeCondition){
		return lambda(t-> true, notification, removeCondition);
	}

	/**
	 * Helper function that creates observer with the help of lambda expressions. For example: <p>
	 *
	 *     <tt>Observer.lambda(t->true, System.out::println)</tt>
	 *
	 * @param condition is used as the notify condition
	 * @param notification is used as the notification method
	 * @param <T> Type of the observed instance
	 * @return a new Oberver.
	 */
	public static <T> Observer<T> lambda(Function<T, Boolean> condition, Consumer<T> notification){
		return lambda(condition, notification, t -> true);
	}
	/**
	 * Creates observer like {@link Observer#lambda(Function, Consumer)} but with the given remove condition function.
	 */
	public static <T> Observer<T> lambda(Function<T, Boolean> condition, Consumer<T> notification, Function<T, Boolean> removeCondition){
		return new Observer<T>() {
			@Override
			public boolean notifyCondition(T t) {
				return condition.apply(t);
			}

			@Override
			public void notification(T t) {
				notification.accept(t);
			}

			@Override
			public boolean removeAfterNotification(T t) { return removeCondition.apply(t); }
		};
	}
}
