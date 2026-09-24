package io.blindado.android

import android.app.Application

/**
 * Sem framework de injeção de dependência (Constituição, Princípio VII — sem abstração
 * especulativa): a composição de dependências é manual e simples, feita em [MainActivity].
 */
class BlindadoApplication : Application()
