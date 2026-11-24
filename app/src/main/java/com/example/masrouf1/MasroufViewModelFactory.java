package com.example.masrouf1;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public final class MasroufViewModelFactory implements ViewModelProvider.Factory {
    private final Context applicationContext;
    public MasroufViewModelFactory(Context context){applicationContext=context.getApplicationContext();}
    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass){
        if(modelClass.isAssignableFrom(MasroufViewModel.class))return modelClass.cast(new MasroufViewModel(new MasroufRepository(applicationContext)));
        throw new IllegalArgumentException("Unsupported ViewModel: "+modelClass.getName());
    }
}
