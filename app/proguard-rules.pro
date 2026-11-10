# Reglas de ProGuard (la compilación release no ofusca por ahora).
# Si se activa minify, mantener los modelos que Gson convierte:
-keep class com.dforge.studio.data.model.** { *; }
