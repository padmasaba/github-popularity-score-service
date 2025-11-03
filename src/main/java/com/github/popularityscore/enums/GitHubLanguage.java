package com.github.popularityscore.enums;

public enum GitHubLanguage {

    JAVA("Java"),
    JAVASCRIPT("JavaScript"),
    TYPESCRIPT("TypeScript"),
    NODE("Node"),
    KOTLIN("Kotlin"),
    GO("Go"),
    C("C"),
    CPLUSPLUS("C++"),
    CSHARP("C#"),
    PYTHON("Python"),
    RUBY("Ruby"),
    SWIFT("Swift"),
    PHP("PHP"),
    HTML("Html"),
    CSS("Css"),
    SHELL("Shell"),
    RUST("Rust"),
    DART("Dart"),
    SCALA("Scala"),
    R("R"),
    OBJECTIVEC("Objective-C"),
    GROOVY("Groovy"),
    PERL("Perl");

    private final String displayName;

    GitHubLanguage(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
