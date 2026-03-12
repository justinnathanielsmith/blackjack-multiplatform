import SwiftUI
import KotlinModules

@main
struct BlackjackApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return IosEntryPointKt.BlackjackViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
