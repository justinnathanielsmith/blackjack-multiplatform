## 2026-04-12 - [Header Interaction Feedback]
**Learning:** Static icons in the header can be improved by adding snappy scale feedback and hover states without using the default ripple, which often clashes with custom glass/metallic backgrounds.
**Action:** Use `animateFloatAsState` with a spring spec and `graphicsLayer` for light-weight, performant interaction feedback on small UI elements.
