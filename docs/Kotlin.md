# Why write backend code in Kotlin and not Java?

* You will write less code
  * less boilerplate, less to read later
* You can express null safety in code
* Easy to make things immutable
* No magic needed with too many annotations
* Frontend often uses TypeScript, which shares more concepts with Kotlin than Java

| **Concept**                  | **Java**                                                                                                                  | **Kotlin**                                                              | **TypeScript**                                                           |
|------------------------------|---------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|--------------------------------------------------------------------------|
| **Variable Declaration**     | `int age = 25;`                                                                                                           | `val age: Int = 25`                                                     | `let age: number = 25`                                                   |
| **Immutable Variable**       | `final int age = 25;`                                                                                                     | `val age: Int = 25`                                                     | `const age: number = 25`                                                 |
| **Type inference**           | `var age = 25;` (only for local variables)                                                                                | `val age = 25` (anywhere, also for return types)                        | `let age = 25` (anywhere, also for return types)                         |
| **Nullable declaration**     | N/A (nulls allowed everywhere)                                                                                            | `name: String?`                                                         | `name?: string` (undefined)                                              |
| **Nullable dereferencing**   | `Optional.ofNullable(person).map(p -> p.name).orElse(null)` (even more difficult with nested nullability)                 | `person?.name`                                                          | `person?.name`                                                           |
| **Nullable fallback**        | `if (person.name != null) person.name; else "";`                                                                          | `person.name ?: ""`                                                     | `person.name ?? ''`                                                      |
| **Function Declaration**     | `int add(int a, int b) { return a + b; }` (only inside of classes)                                                        | `fun add(a: Int, b: Int) = a + b`                                       | `function add(a: number, b: number) { return a + b }`                    |
| **Class Declaration**        | `class Person {}`                                                                                                         | `class Person`                                                          | `class Person {}`                                                        |
| **Data Class**               | `record Person(String name, int age) {}` (no copy constructors)                                                           | `data class Person(val name: String, val age: Int)` with copy() methods | `{name: string, age: number}`, can be copied with `{...object, age: 21}` |
| **Constructor Parameters**   | `public class Service { Dependency dependency; public Service(Dependency dependency) { this.dependency = dependency; } }` | `class Service(val dependency: Dependency)`                             | `class Service { constructor(public dependency: Dependency) {} }`        |
| **Class reference**          | `Person.class`                                                                                                            | `Person::class`                                                         | `Person`                                                                 |
| **Member reference**         | `Person.class.getDeclaredField("name")`                                                                                   | `Person::name` (type-safe reflection)                                   | `Person.prototype.name`                                                  |
| **Inheritance**              | `class Student extends Person {}`                                                                                         | `class Student: Person()`                                               | `class Student extends Person {}`                                        |
| **Interface Implementation** | `class Dog implements Animal {}`                                                                                          | `class Dog: Animal`                                                     | `class Dog implements Animal {}`                                         |
| **Object Creation**          | `Person person = new Person("John", 25);`                                                                                 | `val person = Person("John", 25)`                                       | `let person = new Person("John", 25)`                                    |
| **String Interpolation**     | `String message = "Hello " + name;`                                                                                       | `val message = "Hello $name"`                                           | ``let message = `Hello ${name}``                                         |
| **Default Parameters**       | N/A                                                                                                                       | `fun greet(name: String = "Guest")`                                     | `function greet(name: string = "Guest")`                                 |
| **Extension functions**      | N/A, usually static utility methods with parameters                                                                       | `fun LocalDate.today() = ...`                                           | `interface Date { function today() {...} }`                              |
| **Lambda Expressions**       | `Runnable r = () -> System.out.println("Hi");`                                                                            | `val greet = { println("Hi") }`                                         | `const greet = () => { console.log('Hi') }`                              |
| **Access Modifiers**         | `public`, `protected`, `private`, package-private by default                                                              | `public` (default), `protected`, `private`, `internal`                  | `public` (default), `protected`, `private`                               |
| **Generics**                 | `List<String> list = new ArrayList<>();`                                                                                  | `val list = ArrayList<String>()`                                        | `let list: Array<string> = []`                                           |
| **List creation**            | `List.of(1, 2, 3);`                                                                                                       | `listOf(1, 2, 3)`                                                       | `[1, 2, 3]`                                                              |
| **Map creation**             | `Map.of("key1", 1, "key2", 2);` (null values not allowed)                                                                 | `mapOf("key1" to 1, "key2" to 2)`                                       | `{key1: 1, key2: 2}`                                                     |
| **Value transformation**     | `list.stream().map(i -> i * 2).collect(toList());`                                                                        | `list.map { it * 2 }`                                                   | `list.map(i => i * 2)`                                                   |
| **Semicolons**               | Required                                                                                                                  | Optional                                                                | Optional                                                                 |
| **Readable test names**      | `@Test void myCoolMethodDoesThisAndThat() {}`                                                                             | `@Test fun ``my cool method does this and that``() {}`                  | `test('my cool method does this and that')`                              |
| **Asynchronous Code**        | `CompletableFuture.supplyAsync(() -> ...)` (no syntactic support)                                                         | `suspend` functions built-in                                            | `async function fetchData() { ... }` async/await built-in                |

See [TSGenerator](../json/src/TSGenerator.kt) in klite-json for generating TypeScript interfaces from Kotlin data classes.