package ujfe.signals;

interface SignalDependency {
    AutoCloseable subscribeInvalidation(Runnable listener);
}
