// Bai 03: C# cho nguoi tu Java. Chay tat ca: dotnet run   |   chay mot phan: dotnet run -- linq
using CSharpBasics;

var sections = new (string Name, Func<Task> Run)[]
{
    ("types", Basics.Types),
    ("nulls", Basics.Nulls),
    ("collections", Basics.Collections),
    ("classes", Oop.Classes),
    ("records", Oop.Records),
    ("patterns", Oop.Patterns),
    ("linq", Queries.Linq),
    ("errors", Queries.Errors),
    ("async", Async.Run),
    ("generics", Async.Generics)
};

var wanted = args.FirstOrDefault() ?? "all";
foreach (var (name, run) in sections.Where(s => wanted == "all" || s.Name == wanted))
{
    Console.WriteLine($"===== {name}");
    await run();
}
