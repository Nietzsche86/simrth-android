package id.mts.rth.core.common

/**
 * Wrapper serbaguna untuk state operasi asinkron (Repository & ViewModel).
 */
sealed class ResultState<out T> {
    data object Idle : ResultState<Nothing>()
    data object Loading : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()
    data class Error(val message: String, val throwable: Throwable? = null) : ResultState<Nothing>()

    val isLoading: Boolean get() = this is Loading
    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}
