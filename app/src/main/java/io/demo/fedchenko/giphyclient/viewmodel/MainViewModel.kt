package io.demo.fedchenko.giphyclient.viewmodel

import androidx.lifecycle.*
import io.demo.fedchenko.giphyclient.model.GifModel
import io.demo.fedchenko.giphyclient.repository.GifLoader
import io.demo.fedchenko.giphyclient.repository.GifProvider
import io.demo.fedchenko.giphyclient.repository.SearchGifLoader
import io.demo.fedchenko.giphyclient.repository.TrendingGifLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainViewModel(private var gifProvider: GifProvider) :
    ViewModel() {

    private val isLoadingLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val isLoading: LiveData<Boolean> = isLoadingLiveData
    private val gifModelsLiveData: MutableLiveData<List<GifModel>> = MutableLiveData()
    private val isCloseButtonVisibleLiveData: MutableLiveData<Boolean> = MutableLiveData()
    val isCloseButtonVisible: LiveData<Boolean> = isCloseButtonVisibleLiveData
    private var exceptionListener: (() -> Unit)? = null
    private var keyboardListener: (() -> Unit)? = null

    private var lastTerm = ""

    private var gifLoader: GifLoader = TrendingGifLoader(gifProvider)

    val searchText: MutableLiveData<String> = MutableLiveData()

    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        isLoadingLiveData.value = false
        gifModelsLiveData.value = emptyList()
        isCloseButtonVisibleLiveData.value = false
        searchText.observeForever {
            isCloseButtonVisibleLiveData.value = it.isNotEmpty()
        }
        subscribeToLoader()
    }

    fun search() {
        val trimTerm = searchText.value?.trim() ?: return
        if (trimTerm == "" || trimTerm == lastTerm)
            return
        lastTerm = trimTerm
        keyboardListener?.invoke()
        gifLoader = SearchGifLoader(gifProvider, trimTerm)
        subscribeToLoader()
    }

    fun clean() {
        searchText.value = ""
        if (lastTerm.isNotEmpty())
            getTrending()
    }

    fun getTrending() {
        if (lastTerm.isNotEmpty())
            searchText.value = ""
        lastTerm = ""
        keyboardListener?.invoke()
        gifLoader = TrendingGifLoader(gifProvider)
        subscribeToLoader()
    }

    private fun subscribeToLoader() {
        isLoadingLiveData.value = false
        //scope.cancel()
        gifModelsLiveData.value = emptyList()
        getMoreGifs()
    }

    fun getMoreGifs() {
        if (isLoadingLiveData.value != true) {
            isLoadingLiveData.value = true
            scope.launch {
                val gifs = gifLoader.loadMoreGifs()
                gifModelsLiveData.value = gifs
                isLoadingLiveData.value = false
            }
        }
    }

    fun observeGifModels(lifecycleOwner: LifecycleOwner, observer: Observer<List<GifModel>>) {
        gifModelsLiveData.observe(lifecycleOwner, observer)
    }

    fun registerExceptionsListener(listener: (() -> Unit)) {
        exceptionListener = listener
    }

    fun removeExceptionListener() {
        exceptionListener = null
    }

    fun registerKeyboardListener(listener: (() -> Unit)) {
        keyboardListener = listener
    }

    fun removeKeyboardListener() {
        keyboardListener = null
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}