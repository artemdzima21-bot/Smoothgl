# SmoothGL

Честный мод против микрофризов на OpenGL рендер-пути Minecraft 1.21.4 (Fabric).

Никаких "+200% FPS". Этот мод чинит **конкретные, измеримые** источники
микрозадержек, которые реально существуют в JVM/MC, и **показывает цифрами**,
что именно происходит на твоём ПК — чтобы ты сам мог проверить эффект,
а не верить на слово.

## Что делает

| Компонент | Проблема | Решение |
|-----------|----------|---------|
| `JitWarmup` | Первые 10–30 секунд игры стутерят, потому что C2 догоняется компилировать `Matrix4f`, `BufferBuilder`, `Frustum` и т.д. | На бэкграунд-треде заранее прокручивает горячие циклы JOML/MC, чтобы JIT поднял их в C2 ДО рендера. |
| `BufferPreTouch` | Первая большая `ByteBuffer.allocateDirect` коммитит OS-страницы → стол на main-треде. | На старте выделяет и трогает каждую 4 KiB страницу заранее (по умолчанию 64 MiB). |
| `GcWatcher` | "Иногда фризит" — обычно это GC stop-the-world. | Подписывается на JVM GC notifications и логирует каждую паузу >20 мс с причиной. |
| `StutterProfiler` | Ты не можешь чинить то, что не измеряешь. | Меряет каждый кадр, считает медиану/p99, ловит выбросы (>1.5× медианы) и снимает стек main-треда. |
| `FramePacer` | OpenGL-драйвер по умолчанию буферизует 2–3 кадра вперёд → CPU выдаёт следующий кадр, пока GPU ещё не дорендерил предыдущий. Это даёт неравномерный pacing и +20–50 мс input lag. | После каждого `swapBuffers` ставит GPU fence (`glFenceSync`). Когда очередь fence'ов превышает `maxFramesInFlight` (по умолчанию 1), CPU блокируется на `glClientWaitSync` до окончания GPU. Это **не `glFinish`** — пайплайн не сливается полностью, просто CPU не может уйти больше чем на 1 кадр вперёд. |
| `EntityCuller` | Vanilla рендерит entities в радиусе симуляции, даже если они в 100+ блоков и занимают 1 пиксель. | Перед каждым entity-render проверяет квадрат расстояния до камеры и отрезает дальше `entityCullDistance` (default 64 блока). Player и focused-entity никогда не куллятся. |
| `BlockEntityCuller` | На серверах со скоплениями chest/itemframe/sign рендер block entities становится узким местом — vanilla их не frustum-чекает аккуратно. | Distance-cull для BE по `blockEntityCullDistance` (default 48 блоков). На крупных шопах/фермах даёт реальный +FPS. |
| `ParticleCuller` | TNT, лава, редстоун-фермы выдают сотни-тысячи частиц, и `Particle.buildGeometry` зовётся на каждой каждый кадр. | Перед `buildGeometry` проверяет квадрат расстояния от камеры до bounding box центра частицы и cancel'ит если дальше `particleCullDistance` (default 32 блока). |

## Использование

**Команды (чат):**

- `/smoothgl` — статус: frames, stutters, median, p99, GC паузы.
- `/smoothgl dump` — топ-5 худших фризов с трейсом + полный лог в `logs/latest.log`.
- `/smoothgl breakdown` — разбивка кадра по стадиям: world / gui / pacer-wait / other (с гистограммой).
- **F7** — включить/выключить живой HUD-оверлей с разбивкой кадра (привязка перенастраивается в `Options → Controls → SmoothGL`).
- `/smoothgl reset` — сбросить статистику.
- `/smoothgl set <ключ> <значение>` — поменять настройку и сохранить.

**Конфиг:** `config/smoothgl.properties`

```
enableProfiler=true
enableJitWarmup=true
enableGcWatcher=true
enableBufferPreTouch=true
enableFramePacer=true
maxFramesInFlight=1
enableEntityCull=true
entityCullDistance=64
enableBlockEntityCull=true
blockEntityCullDistance=48
enableParticleCull=true
particleCullDistance=32
stutterRatio=1.5
stutterFloorMs=8.0
preTouchMiB=64
```

`maxFramesInFlight=1` — самый строгий и низколатентный режим (рекомендуется).
`=2` — компромисс, если упёрся в throughput на слабой GPU.
`=3` — практически отключает пейсер (поведение драйвера по умолчанию).

## ⚠️ Самое главное: JVM-флаги

Никакой мод не починит GC-паузы лучше, чем правильные JVM-флаги.
**Добавь в лаунчере (TLauncher / PrismLauncher / .bat):**

### Java 21 + 8 GiB RAM (рекомендуется):
```
-Xms8G -Xmx8G
-XX:+UseG1GC
-XX:+ParallelRefProcEnabled
-XX:+UnlockExperimentalVMOptions
-XX:+DisableExplicitGC
-XX:+AlwaysPreTouch
-XX:G1NewSizePercent=30
-XX:G1MaxNewSizePercent=40
-XX:G1HeapRegionSize=8M
-XX:G1ReservePercent=20
-XX:G1HeapWastePercent=5
-XX:G1MixedGCCountTarget=4
-XX:InitiatingHeapOccupancyPercent=15
-XX:G1MixedGCLiveThresholdPercent=90
-XX:G1RSetUpdatingPauseTimePercent=5
-XX:SurvivorRatio=32
-XX:MaxTenuringThreshold=1
-XX:MaxGCPauseMillis=37
```

### Альтернатива — ZGC (Java 21, минимум stop-the-world, нужно ≥6 GiB):
```
-Xms8G -Xmx8G
-XX:+UseZGC
-XX:+ZGenerational
-XX:+AlwaysPreTouch
-XX:+DisableExplicitGC
```

`-XX:+AlwaysPreTouch` и `-XX:+DisableExplicitGC` — это **то самое**, что
устраняет основную часть микрофризов от GC. SmoothGL дополнительно
покажет тебе через `/smoothgl`, работают они или нет.

## Что мод НЕ делает (намеренно)

- Не патчит шейдеры и пайплайны рендера — этим занимаются Sodium/VulkanMod.
- Не обещает FPS буст — он не возникает из ниоткуда.
- Не лезет в чанк-меш билдер MC — слишком хрупко между версиями yarn.
- Не вызывает `System.gc()` "для оптимизации" — это анти-паттерн.

## Сборка

```
./gradlew build
```

Артефакт: `build/libs/smoothgl-0.1.0.jar`

Требования: JDK 21, Fabric Loader 0.16.9+, Fabric API.
