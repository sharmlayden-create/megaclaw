package com.megaclaw.ui;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  @Override
  public ChatViewModel get() {
    return newInstance();
  }

  public static ChatViewModel_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ChatViewModel newInstance() {
    return new ChatViewModel();
  }

  private static final class InstanceHolder {
    private static final ChatViewModel_Factory INSTANCE = new ChatViewModel_Factory();
  }
}
