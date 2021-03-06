package com.meiji.daily.module.shareadd;

import com.meiji.daily.di.component.AppComponent;
import com.meiji.daily.di.scope.ActivityScoped;

import dagger.Component;

/**
 * Created by Meiji on 2017/12/28.
 */
@ActivityScoped
@Component(dependencies = AppComponent.class)
public interface ShareAddComponent {

    void inject(ShareAddActivity shareAddActivity);
}
