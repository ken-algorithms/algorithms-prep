# Bài 03 — C# và .NET 8 cho người đến từ Java: 10 nhóm cú pháp dùng ngay, phần nào cũng chạy được

> **Bạn sẽ làm được:** đọc và viết C# hằng ngày: kiểu dữ liệu, null an toàn, collection, class/record, pattern
> matching, LINQ, exception, `async/await`, generic. Mỗi phần có code chạy được, output thật, đối chiếu Java và bài tập.
>
> **Thời lượng:** đọc 45 phút · gõ lại và chạy 60 phút
>
> **Code:** [`lab/playground/CSharpBasics/`](lab/playground/CSharpBasics/Program.cs) · output thật:
> [`results/csharp-basics.txt`](lab/playground/results/csharp-basics.txt)
>
> **Trước đó:** [Bài 02](02-rider-du-an-dau-tien.md) · **Tiếp theo:** [Bài 04 — Debug trong Rider](04-debug-trong-rider.md)

---

## 0. Cách học bài này

Mở `lab/playground/Playground.sln` trong Rider (hoặc dùng console app của bài 02 rồi dán code vào). Mỗi phần chạy riêng được:

```bash
cd tutorial/dotnet8-rider/lab/playground/CSharpBasics
dotnet run              # chạy cả 10 phần
dotnet run -- linq      # chỉ chạy phần linq
```

Trong Rider: **Run → Edit Configurations…** → `CSharpBasics` → **Program arguments** = `linq` → ⌃R.

📄 **`lab/playground/CSharpBasics/Program.cs`**

```csharp
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
```

Ngay file đầu đã có ba thứ Java không có: mảng **tuple có tên** `(string Name, Func<Task> Run)[]`, **tách tuple**
trong `foreach (var (name, run) in ...)`, và `await` trực tiếp ở top-level.

---

## 1. Bản đồ Java → C#: những khác biệt dễ vấp nhất

| Java | C# | Ghi chú |
|---|---|---|
| `String`, `Object`, `Integer` | `string`, `object`, `int` | `int` là alias của `System.Int32`; **không** có boxing kiểu `Integer` lẫn lộn |
| `a.equals(b)` với chuỗi | `a == b` | `==` trên `string` so sánh **nội dung** |
| getter/setter | **property** `public decimal Price { get; private set; }` | gọi như field: `order.Price` |
| Lombok `@Value`, `record` | `record Money(decimal Amount, string Currency);` | `with` để tạo bản sao |
| `final` (class/method) | `sealed` | |
| `final` field | `readonly` (field), `init` (property), `const` (hằng biên dịch) | |
| package-private | `internal` | phạm vi là **assembly** (project), không phải package |
| `Optional<T>` | `T?` + `?.` `??` `??=` | compiler kiểm tra null (Nullable reference types) |
| `try (var r = ...)` | `using var r = ...;` | |
| checked exception (`throws`) | **không có** | mọi exception đều unchecked |
| `List<Integer>` (type erasure) | `List<int>` (generic **giữ kiểu lúc chạy**) | `List<int>` không boxing, `typeof(T)` dùng được |
| Stream API | **LINQ** | `Where` = `filter`, `Select` = `map` |
| `CompletableFuture<T>` | `Task<T>` + `async/await` | |
| `@Annotation` | `[Attribute]` | `[HttpGet]`, `[Required]`, `[Fact]` |
| functional interface | delegate: `Func<T,R>`, `Action<T>` | |
| varargs `int... xs` | `params int[] xs` | |
| `BigDecimal` | `decimal` (kiểu có sẵn, có toán tử) | `0.1m + 0.2m == 0.3m` |
| `enum` có field/method | `enum` chỉ là số nguyên có tên | muốn hành vi thì dùng class/record hoặc extension method |
| tên method `camelCase` | **`PascalCase`** cho method, property, class; `camelCase` cho biến cục bộ, tham số; `_camelCase` cho field private | quy ước bắt buộc phải theo, Rider gạch chân nếu sai |

---

## 2. Kiểu dữ liệu, số, chuỗi (`dotnet run -- types`)

**Lý thuyết.** C# có hai nhóm kiểu: **value type** (`int`, `decimal`, `bool`, `DateTime`, `struct`, `enum`) nằm trực
tiếp trong biến, gán là **sao chép**; **reference type** (`class`, `string`, mảng, `record`) biến chỉ giữ tham chiếu.
Tiền luôn dùng `decimal` (cơ số 10, 28–29 chữ số), không dùng `double`. `var` là suy kiểu lúc biên dịch, vẫn là kiểu tĩnh.

✂️ **`lab/playground/CSharpBasics/Basics.cs`** (trích)

```csharp
    public static Task Types()
    {
        int count = 3;                 // System.Int32, giong int cua Java
        long big = 3_000_000_000;      // dau _ chi de doc cho de
        decimal price = 150_000m;      // tien LUON dung decimal (nhu BigDecimal), hau to m
        double ratio = 0.1 + 0.2;      // double: nhanh nhung sai so nhi phan
        var name = "OrderFlow";        // var: compiler suy ra string, van la kieu tinh
        Console.WriteLine($"count={count}, big={big}, price={price}, name={name}");
        Console.WriteLine($"0.1 + 0.2 as double  = {ratio:R}");
        Console.WriteLine($"0.1 + 0.2 as decimal = {0.1m + 0.2m}");

        string a = "hello";
        string b = string.Concat("hel", "lo"); // tao chuoi moi luc chay
        Console.WriteLine($"a == b -> {a == b}  (C# so sanh NOI DUNG chuoi bang ==, Java phai dung equals)");

        var json = """{"sku": "BOOK-001", "qty": 2}"""; // raw string (C# 11): khong can escape dau "
        Console.WriteLine(json);

        var vi = CultureInfo.GetCultureInfo("vi-VN");
        Console.WriteLine($"vi-VN N0: {360_000m.ToString("N0", vi)} | C0: {360_000m.ToString("C0", vi)} | invariant: {360_000m.ToString("N0", CultureInfo.InvariantCulture)}");
        return Task.CompletedTask;
    }
```

Output thật:

```text
===== types
count=3, big=3000000000, price=150000, name=OrderFlow
0.1 + 0.2 as double  = 0.30000000000000004
0.1 + 0.2 as decimal = 0.3
a == b -> True  (C# so sanh NOI DUNG chuoi bang ==, Java phai dung equals)
{"sku": "BOOK-001", "qty": 2}
vi-VN N0: 360.000 | C0: 360.000 ₫ | invariant: 360,000
```

- `$"..."` là **chuỗi nội suy** (như `String.format` nhưng viết thẳng biến vào `{}`), `{ratio:R}` là định dạng.
- `"""..."""` (raw string): viết JSON/SQL không cần escape. Có thêm `$"""..."""` để vừa raw vừa nội suy.
- Định dạng số **phụ thuộc culture**: cùng `N0`, máy vùng Việt Nam in `360.000`, server Linux in `360,000`. Chuỗi
  ghi vào DB/log/message nên dùng `CultureInfo.InvariantCulture` (OrderFlow làm vậy ở `Order.Process`).

**Java tương đương:** `new BigDecimal("0.1").add(new BigDecimal("0.2"))` → C# chỉ cần `0.1m + 0.2m`.

---

## 3. Null an toàn (`dotnet run -- nulls`)

**Lý thuyết.** Với `<Nullable>enable</Nullable>` (mặc định trong template .NET 8), `string` nghĩa là **không được
null**, `string?` nghĩa là **có thể null**. Compiler cảnh báo khi bạn dùng `string?` mà chưa kiểm tra. Đây là kiểm
tra **lúc biên dịch** (không chặn runtime), nhưng bắt được phần lớn `NullReferenceException` trước khi chạy.

✂️ **`lab/playground/CSharpBasics/Basics.cs`** (trích)

```csharp
    public static Task Nulls()
    {
        string? maybe = FindEmail(42);                  // string? = co the null; string = compiler hieu la khong null
        Console.WriteLine($"maybe?.Length -> {maybe?.Length.ToString() ?? "null"}"); // ?. : null thi ca bieu thuc la null
        Console.WriteLine(maybe ?? "(no email)");      // ?? : gia tri thay the khi null
        maybe ??= "default@example.com";               // ??= : chi gan khi dang null
        Console.WriteLine(maybe);

        int? quantity = null;                          // kieu gia tri co the null: Nullable<int>
        Console.WriteLine($"HasValue={quantity.HasValue}, GetValueOrDefault(1)={quantity.GetValueOrDefault(1)}");

        if (FindEmail(1) is { } email)                 // khong null thi gan vao bien email
            Console.WriteLine($"found {email}");
        return Task.CompletedTask;

        static string? FindEmail(int id) => id == 1 ? "an@example.com" : null;
    }
```

```text
===== nulls
maybe?.Length -> null
(no email)
default@example.com
HasValue=False, GetValueOrDefault(1)=1
found an@example.com
```

| Toán tử | Nghĩa | Java |
|---|---|---|
| `x?.Length` | `x` null thì cả biểu thức là null | `Optional.ofNullable(x).map(String::length)` |
| `x ?? "mặc định"` | null thì lấy vế phải | `Objects.requireNonNullElse(x, "...")` |
| `x ??= v` | chỉ gán khi `x` đang null | |
| `x!` | "tôi chắc chắn không null", tắt cảnh báo | dùng ít thôi; sai là `NullReferenceException` |
| `if (x is { } v)` | không null thì gán vào `v` | |

`static string? FindEmail(...)` đặt **bên trong** method: *local function*, chỉ dùng được trong method đó.

---

## 4. Collection (`dotnet run -- collections`)

✂️ **`lab/playground/CSharpBasics/Basics.cs`** (trích)

```csharp
    public static Task Collections()
    {
        int[] numbers = [5, 3, 8, 1];                  // collection expression (C# 12)
        List<string> skus = ["BOOK-001", "PEN-002"];
        skus.Add("MUG-003");
        Console.WriteLine($"numbers: {string.Join(", ", numbers)} | skus: {string.Join(", ", skus)} (Count={skus.Count})");

        var stock = new Dictionary<string, int>(StringComparer.OrdinalIgnoreCase) // khoa khong phan biet hoa thuong
        {
            ["BOOK-001"] = 10,
            ["PEN-002"] = 0
        };
        Console.WriteLine($"stock[\"book-001\"] = {stock["book-001"]}");
        if (!stock.TryGetValue("MUG-003", out var mugs))   // TryGetValue: khong nem KeyNotFoundException
            Console.WriteLine($"MUG-003 not in stock (mugs={mugs})");

        foreach (var (sku, qty) in stock)              // tach KeyValuePair thanh hai bien
            Console.WriteLine($"  {sku}: {qty}");

        var unique = new HashSet<string> { "a", "b", "a" };
        int[] sorted = [.. numbers.Order()];           // .. trai cac phan tu vao mang moi
        Console.WriteLine($"HashSet count={unique.Count} | sorted: {string.Join(", ", sorted)}");
        return Task.CompletedTask;
    }
```

```text
===== collections
numbers: 5, 3, 8, 1 | skus: BOOK-001, PEN-002, MUG-003 (Count=3)
stock["book-001"] = 10
MUG-003 not in stock (mugs=0)
  BOOK-001: 10
  PEN-002: 0
HashSet count=2 | sorted: 1, 3, 5, 8
```

| Java | C# |
|---|---|
| `ArrayList<T>`, `List.of(...)` | `List<T>`, `[a, b]` (C# 12), `IReadOnlyList<T>` cho tham số/kết quả chỉ đọc |
| `HashMap<K,V>` | `Dictionary<K,V>`; `dict[key]` **ném** `KeyNotFoundException` nếu thiếu, `TryGetValue` thì không |
| `HashSet<T>` | `HashSet<T>` |
| `list.size()` | `list.Count` (property), mảng dùng `.Length` |
| `Iterable<T>` | `IEnumerable<T>` |

`StringComparer.OrdinalIgnoreCase` sẽ quay lại ở bài 04: chính nó sửa lỗi `KeyNotFoundException: 'book-001'`.

---

## 5. Class, interface, property (`dotnet run -- classes`)

**Lý thuyết.**

- **Property** thay getter/setter: `public decimal Balance { get; private set; }` đọc được từ ngoài, chỉ ghi trong class.
- **Primary constructor** (C# 12, mới trong .NET 8): `class BankAccount(string number, decimal initialBalance)`;
  tham số dùng được trong cả class, khỏi khai field và gán tay. Bài 06–09 dùng kiểu này cho mọi class nhận DI.
- `required` + `init`: bắt buộc gán lúc tạo, sau đó không đổi được.
- **Extension method**: thêm method cho kiểu có sẵn (ở đây `decimal.ToVnd()`) mà không sửa kiểu đó.

✂️ **`lab/playground/CSharpBasics/Oop.cs`** (trích)

```csharp
// primary constructor (C# 12): tham so dung duoc trong ca class, khong can khai field + gan tay
public class BankAccount(string number, decimal initialBalance)
{
    public string Number { get; } = number;
    public decimal Balance { get; private set; } = initialBalance; // doc tu ngoai duoc, ghi chi trong class

    public void Deposit(decimal amount)
    {
        ArgumentOutOfRangeException.ThrowIfNegativeOrZero(amount); // helper moi cua .NET 8
        Balance += amount;
    }

    public void Withdraw(decimal amount)
    {
        if (amount > Balance)
            throw new InvalidOperationException($"insufficient funds: balance {Balance}, requested {amount}");
        Balance -= amount;
    }

    public override string ToString() => $"{Number}: {Balance.ToVnd()}";
}

public interface INotifier
{
    void Send(string message);
}

public class EmailNotifier : INotifier
{
    public void Send(string message) => Console.WriteLine($"[email] {message}");
}

public class SmsNotifier : INotifier
{
    public void Send(string message) => Console.WriteLine($"[sms] {message}");
}

public class Product
{
    public required string Sku { get; init; }  // required: quen gan luc tao la loi compile
    public required string Name { get; init; }
    public decimal Price { get; init; }
}
```

✂️ **`lab/playground/CSharpBasics/Oop.cs`** (trích)

```csharp
public static class MoneyExtensions
{
    // extension method: them method cho kieu co san (decimal) ma khong sua kieu do
    public static string ToVnd(this decimal amount) =>
        amount.ToString("N0", CultureInfo.GetCultureInfo("vi-VN")) + " ₫";
}
```

Dùng chúng:

✂️ **`lab/playground/CSharpBasics/Oop.cs`** (trích)

```csharp
    public static Task Classes()
    {
        var account = new BankAccount("ACC-1", initialBalance: 100_000m); // tham so co ten, nhu Kotlin
        account.Deposit(50_000m);
        Console.WriteLine(account);
        try
        {
            account.Withdraw(1_000_000m);
        }
        catch (InvalidOperationException ex)
        {
            Console.WriteLine($"error: {ex.Message}");
        }

        INotifier[] notifiers = [new EmailNotifier(), new SmsNotifier()];
        foreach (var notifier in notifiers)
            notifier.Send("Order confirmed");

        var product = new Product { Sku = "BOOK-001", Name = "Clean Code", Price = 150_000m }; // object initializer
        // product.Sku = "X";  // loi compile CS8852: init-only, chi gan luc tao
        Console.WriteLine($"{product.Sku} {product.Name} {product.Price.ToVnd()}");      // ToVnd: extension method
        return Task.CompletedTask;
    }
```

```text
===== classes
ACC-1: 150.000 ₫
error: insufficient funds: balance 150000, requested 1000000
[email] Order confirmed
[sms] Order confirmed
BOOK-001 Clean Code 150.000 ₫
```

`=> biểu thức` là *expression-bodied member*: method một dòng, như lambda. Quy ước tên interface bắt đầu bằng `I`.

---

## 6. Record (`dotnet run -- records`)

**Lý thuyết.** Một dòng `record Money(decimal Amount, string Currency);` sinh ra: constructor, property `init`,
`Equals`/`GetHashCode` **theo giá trị**, `ToString` đẹp, `Deconstruct`. Dùng cho DTO, message, value object. Bài 06
dùng record cho request/response của API, bài 08 dùng cho message `OrderCreated`.

✂️ **`lab/playground/CSharpBasics/Oop.cs`** (trích)

```csharp
    public static Task Records()
    {
        var a = new Money(100_000m, "VND");
        var b = new Money(100_000m, "VND");
        Console.WriteLine($"a == b -> {a == b}  (record so sanh theo gia tri)");
        Console.WriteLine(a);                          // ToString tu sinh
        var c = a with { Amount = 250_000m };          // ban sao, doi mot truong; a giu nguyen
        Console.WriteLine($"{c} | a still {a.Amount}");
        var (amount, currency) = c;                    // deconstruct
        Console.WriteLine($"amount={amount}, currency={currency}");
        return Task.CompletedTask;
    }
```

```text
===== records
a == b -> True  (record so sanh theo gia tri)
Money { Amount = 100000, Currency = VND }
Money { Amount = 250000, Currency = VND } | a still 100000
amount=250000, currency=VND
```

---

## 7. Pattern matching (`dotnet run -- patterns`)

**Lý thuyết.** `switch` **biểu thức** trả về giá trị, mỗi nhánh là một *pattern*: kiểu (`int n`), điều kiện
(`when n < 0`), property (`{ Length: 0 }`), so sánh (`>= 500_000m`), danh sách (`[var first, .., var last]`), `_` là
"còn lại". Compiler cảnh báo nếu thiếu trường hợp.

✂️ **`lab/playground/CSharpBasics/Oop.cs`** (trích)

```csharp
        static string Describe(object? value) => value switch // switch expression: tra ve gia tri
        {
            null => "null",
            int n when n < 0 => "negative int",
            int n => $"int {n}",
            string { Length: 0 } => "empty string",          // property pattern
            string s => $"string '{s}'",
            Money { Currency: "VND", Amount: > 1_000_000m } => "big VND amount",
            Money m => $"money {m.Amount} {m.Currency}",
            int[] and [var first, .., var last] => $"array {first}..{last}", // list pattern (C# 11)
            _ => $"other: {value.GetType().Name}"
        };

        static decimal ShippingFee(decimal total) => total switch // relational pattern
        {
            >= 500_000m => 0m,
            >= 200_000m => 15_000m,
            _ => 30_000m
        };
```

```text
===== patterns
null                         <- null
negative int                 <- -5
int 42                       <- 42
empty string                 <- 
string 'hi'                  <- hi
big VND amount               <- Money { Amount = 2000000, Currency = VND }
money 5 USD                  <- Money { Amount = 5, Currency = USD }
array 1..3                   <- System.Int32[]
other: Double                <- 3.14
shipping fee for 120.000 ₫: 30.000 ₫
shipping fee for 250.000 ₫: 15.000 ₫
shipping fee for 800.000 ₫: 0 ₫
```

Một lần viết sai mà compiler bắt được ngay khi soạn bài: `int[] [var first, .., var last]` báo
`CS1003: Syntax error, ',' expected`. Ghép pattern kiểu với pattern danh sách phải dùng `and`.

---

## 8. LINQ (`dotnet run -- linq`)

**Lý thuyết.** LINQ là bộ method mở rộng trên `IEnumerable<T>`, tương đương Stream API. Hai điểm khác cần nhớ:

1. **Thực thi trì hoãn** (deferred): `Where`/`Select` chỉ *định nghĩa* truy vấn; nó chạy khi bạn duyệt (`foreach`,
   `ToList()`, `First()`, `Sum()`…). Như Stream không có terminal operation thì không chạy.
2. Cùng cú pháp đó với EF Core (bài 07) được **dịch thành SQL**. Viết `db.Orders.Where(o => o.Status == ...)` là sinh `WHERE`.

✂️ **`lab/playground/CSharpBasics/Queries.cs`** (trích)

```csharp
    public static Task Linq()
    {
        // LINQ ~ Java Stream: Where = filter, Select = map, Sum/Count/Any = reduce
        var confirmedTotal = Orders.Where(o => o.Status == "Confirmed").Sum(o => o.Total);
        Console.WriteLine($"confirmed total: {confirmedTotal.ToVnd()}");

        var byCustomer = Orders
            .GroupBy(o => o.Customer)
            .Select(g => new { Customer = g.Key, Count = g.Count(), Total = g.Sum(o => o.Total) }) // kieu vo danh
            .OrderByDescending(x => x.Total);
        foreach (var row in byCustomer)
            Console.WriteLine($"  {row.Customer,-5} {row.Count} order(s) {row.Total.ToVnd(),16}");

        var biggest = Orders.MaxBy(o => o.Total)!;
        Console.WriteLine($"biggest: #{biggest.Id} | any rejected: {Orders.Any(o => o.Status == "Rejected")} | " +
                          $"first pending: #{Orders.First(o => o.Status == "Pending").Id}");

        var byId = Orders.ToDictionary(o => o.Id);
        Console.WriteLine($"byId[3].Customer = {byId[3].Customer}");

        // deferred execution: dinh nghia query KHONG chay gi ca; chi chay khi duyet (First, ToList, foreach)
        var query = Orders.Where(o =>
        {
            Console.WriteLine($"  checking #{o.Id}");
            return o.Total > 500_000m;
        });
        Console.WriteLine("query defined, nothing ran yet");
        Console.WriteLine($"first big order: #{query.First().Id}");
        return Task.CompletedTask;
    }
```

```text
===== linq
confirmed total: 1.380.000 ₫
  binh  2 order(s)     60.900.000 ₫
  an    2 order(s)        480.000 ₫
  chi   1 order(s)         45.000 ₫
biggest: #2 | any rejected: True | first pending: #4
byId[3].Customer = an
query defined, nothing ran yet
  checking #1
  checking #2
first big order: #2
```

Dòng `checking` chứng minh thực thi trì hoãn: định nghĩa query không in gì; `First()` chỉ duyệt tới phần tử đầu tiên khớp (#2).

| Stream (Java) | LINQ (C#) |
|---|---|
| `filter` | `Where` |
| `map` | `Select` |
| `flatMap` | `SelectMany` |
| `collect(groupingBy(...))` | `GroupBy` |
| `sorted(comparing(...))` | `OrderBy` / `OrderByDescending` / `ThenBy` |
| `findFirst().orElseThrow()` | `First()` (ném nếu rỗng) / `FirstOrDefault()` (trả null/0) |
| `anyMatch` / `allMatch` | `Any` / `All` |
| `collect(toList())` | `ToList()` |
| `collect(toMap(...))` | `ToDictionary(...)` |
| `max(comparing(...))` | `MaxBy(...)` |

---

## 9. Exception và `using` (`dotnet run -- errors`)

**Lý thuyết.** Không có checked exception: không có `throws`, không bị ép `catch`. **Exception filter** `when (...)`
chỉ bắt khi điều kiện đúng (không bắt rồi ném lại, stack trace giữ nguyên). `using` gọi `Dispose()` khi ra khỏi phạm
vi, như try-with-resources. Input của người dùng thì dùng `TryParse`, đừng dùng exception để rẽ nhánh.

✂️ **`lab/playground/CSharpBasics/Queries.cs`** (trích)

```csharp
    public static Task Errors()
    {
        try
        {
            int.Parse("abc");
        }
        catch (FormatException ex)
        {
            Console.WriteLine($"FormatException: {ex.Message}");
        }

        try
        {
            throw new HttpRequestException("gateway timeout", null, HttpStatusCode.GatewayTimeout);
        }
        catch (HttpRequestException ex) when (ex.StatusCode == HttpStatusCode.GatewayTimeout) // exception filter
        {
            Console.WriteLine("caught only because the filter matched: 504 is retryable");
        }

        Console.WriteLine(int.TryParse("42", out var n) ? $"TryParse ok: {n}" : "not a number"); // input nguoi dung: TryParse

        using (var first = new DemoResource("connection-1")) // using = try-with-resources cua Java
        {
            Console.WriteLine("  working with connection-1");
        }
        using var second = new DemoResource("connection-2");  // using declaration: Dispose o cuoi method
        Console.WriteLine("  end of the method body");
        return Task.CompletedTask;
    }
```

```text
===== errors
FormatException: The input string 'abc' was not in a correct format.
caught only because the filter matched: 504 is retryable
TryParse ok: 42
  open connection-1
  working with connection-1
  dispose connection-1
  open connection-2
  end of the method body
  dispose connection-2
```

`out var n`: tham số **out**, method gán giá trị vào biến của bạn. Mẫu `TryXxx(input, out var result)` có khắp .NET
(`TryGetValue`, `TryParse`, `TryAdd`).

---

## 10. `async`/`await` (`dotnet run -- async`)

**Lý thuyết.** `Task<T>` ≈ `CompletableFuture<T>`. `await` *chờ không chặn thread*: trong lúc đợi DB/HTTP, thread
được trả về thread pool phục vụ request khác. Ba quy tắc dùng suốt bộ bài:

1. Method có `await` thì khai `async Task` / `async Task<T>`, tên kết thúc bằng `Async`.
2. **Luôn `await`** Task bạn tạo ra. Quên `await` là lỗi thật ở bài 04 (file báo cáo không được ghi).
3. Truyền `CancellationToken` xuyên các tầng: client huỷ request hoặc app tắt thì mọi thứ dừng theo.

✂️ **`lab/playground/CSharpBasics/Async.cs`** (trích)

```csharp
    public static async Task Run()
    {
        var sw = Stopwatch.StartNew();
        string[] skus = ["BOOK-001", "PEN-002", "MUG-03"];

        // goi lan luot: tong ~ 3 x 300 ms
        foreach (var sku in skus)
            await GetStockAsync(sku, CancellationToken.None);
        Console.WriteLine($"sequential: ~{Round(sw.ElapsedMilliseconds)} ms");

        // goi song song: tao ca 3 Task truoc roi moi await -> tong ~ 300 ms
        sw.Restart();
        int[] stocks = await Task.WhenAll(skus.Select(sku => GetStockAsync(sku, CancellationToken.None)));
        Console.WriteLine($"parallel:   ~{Round(sw.ElapsedMilliseconds)} ms, stocks = {string.Join(", ", stocks)}");

        // huy: CancellationToken di xuyen moi tang (controller -> service -> EF Core -> Npgsql)
        using var cts = new CancellationTokenSource(TimeSpan.FromMilliseconds(100));
        try
        {
            await GetStockAsync("SLOW-SKU", cts.Token);
        }
        catch (OperationCanceledException)
        {
            Console.WriteLine("cancelled after ~100 ms instead of waiting 300 ms");
        }

        static long Round(long ms) => (ms + 50) / 100 * 100;
    }

    private static async Task<int> GetStockAsync(string sku, CancellationToken ct)
    {
        await Task.Delay(300, ct); // gia lap goi DB/HTTP; trong luc cho, thread duoc tra lai thread pool
        return sku.Length;
    }
```

```text
===== async
sequential: ~900 ms
parallel:   ~300 ms, stocks = 8, 7, 6
cancelled after ~100 ms instead of waiting 300 ms
```

**Java tương đương:** `CompletableFuture.allOf(f1, f2, f3).join()` ≈ `await Task.WhenAll(...)`. Khác biệt lớn: C#
không cần thread riêng cho mỗi tác vụ đang chờ, và cú pháp `await` đọc như code tuần tự.

---

## 11. Generic và delegate (`dotnet run -- generics`)

✂️ **`lab/playground/CSharpBasics/Async.cs`** (trích)

```csharp
    public static Task Generics()
    {
        // generic method + rang buoc: T phai so sanh duoc
        Console.WriteLine($"Max(3, 9, 4) = {Max(3, 9, 4)} | Max(\"kiwi\", \"apple\") = {Max("kiwi", "apple")}");

        // delegate co san: Func<vao, ra> va Action<vao> ~ Function/Consumer cua Java
        Func<decimal, decimal> withVat = amount => amount * 1.08m;
        Action<string> log = message => Console.WriteLine($"[log] {message}");
        log($"price with 8% VAT: {withVat(100_000m)}");

        var cache = new Cache<string, decimal>();
        Console.WriteLine($"first call: {cache.GetOrAdd("BOOK-001", _ => 150_000m)}, cached: {cache.Count}");
        return Task.CompletedTask;

        static T Max<T>(params T[] items) where T : IComparable<T>
        {
            var best = items[0];
            foreach (var item in items)
                if (item.CompareTo(best) > 0)
                    best = item;
            return best;
        }
    }
```

```text
===== generics
Max(3, 9, 4) = 9 | Max("kiwi", "apple") = kiwi
[log] price with 8% VAT: 108000.00
first call: 150000, cached: 1
```

`where T : IComparable<T>` ≈ `<T extends Comparable<T>>`. Khác Java: generic C# **giữ kiểu lúc chạy**, nên
`List<int>` lưu số nguyên thật (không boxing), và `new T()`, `typeof(T)` dùng được khi có ràng buộc phù hợp.

---

## 12. Bài tập (gõ tay, đừng copy)

Làm trong console app của bài 02. Lời giải đã chạy thật, output ghi bên dưới.

1. **types:** 3 cuốn sách giá 149.900 ₫, giảm 10%. In tổng theo định dạng vi-VN.
2. **nulls:** viết `string Greeting(string? name)`: null hoặc toàn khoảng trắng thì `"Xin chào, khách"`.
3. **collections:** đếm số lần mỗi SKU xuất hiện trong danh sách quét mã vạch.
4. **records:** `record CartLine(string Sku, int Quantity, decimal UnitPrice)` có property tính `Total`.
5. **patterns:** phân loại HTTP status: 2xx `success`, 4xx `client error`, 5xx `server error`, còn lại `other`.
6. **linq:** tổng tiền theo trạng thái đơn.
7. **errors:** `int? ParseQuantity(string input)`: trả null nếu không phải số nguyên dương.
8. **async:** gọi 3 "API" giả song song (50 ms, 80 ms, 500 ms) với timeout chung 200 ms; in cái nào xong, cái nào timeout.

<details>
<summary>Lời giải (đã chạy) và output</summary>

```csharp
using System.Globalization;

var vi = CultureInfo.GetCultureInfo("vi-VN");

// 1. types
decimal unitPrice = 149_900m;
int quantity = 3;
decimal total = unitPrice * quantity * (1 - 0.10m);
Console.WriteLine($"1) {total.ToString("N0", vi)} ₫");

// 2. nulls
static string Greeting(string? name) => $"Xin chào, {(string.IsNullOrWhiteSpace(name) ? "khách" : name.Trim())}";
Console.WriteLine($"2) {Greeting(null)} | {Greeting("  ")} | {Greeting(" An ")}");

// 3. collections
List<string> scanned = ["BOOK-001", "PEN-002", "BOOK-001", "MUG-003", "BOOK-001"];
var counts = new Dictionary<string, int>();
foreach (var sku in scanned)
    counts[sku] = counts.TryGetValue(sku, out var n) ? n + 1 : 1;
Console.WriteLine($"3) {string.Join(", ", counts.Select(kv => $"{kv.Key}={kv.Value}"))}");

// 4. records
var line = new CartLine("BOOK-001", 3, 149_900m);
Console.WriteLine($"4) {line} total={line.Total}");

// 5. patterns
static string Kind(int status) => status switch
{
    >= 200 and < 300 => "success",
    >= 400 and < 500 => "client error",
    >= 500 => "server error",
    _ => "other"
};
Console.WriteLine($"5) {Kind(201)}, {Kind(404)}, {Kind(503)}, {Kind(302)}");

// 6. linq
var orders = new[] { (Status: "Confirmed", Total: 360_000m), (Status: "Rejected", Total: 60_000_000m), (Status: "Confirmed", Total: 120_000m) };
var byStatus = orders.GroupBy(o => o.Status).Select(g => $"{g.Key}: {g.Sum(o => o.Total).ToString("N0", vi)}");
Console.WriteLine($"6) {string.Join(" | ", byStatus)}");

// 7. errors
static int? ParseQuantity(string input) => int.TryParse(input, out var q) && q > 0 ? q : null;
Console.WriteLine($"7) {ParseQuantity("3")}, {ParseQuantity("-1")?.ToString() ?? "invalid"}, {ParseQuantity("ba")?.ToString() ?? "invalid"}");

// 8. async
static async Task<string> CallAsync(string name, int ms, CancellationToken ct) { await Task.Delay(ms, ct); return name; }
using var cts = new CancellationTokenSource(TimeSpan.FromMilliseconds(200));
var calls = new[] { CallAsync("stock", 50, cts.Token), CallAsync("price", 80, cts.Token), CallAsync("slow-shipping", 500, cts.Token) };
try { await Task.WhenAll(calls); }
catch (OperationCanceledException) { }
Console.WriteLine($"8) {string.Join(", ", calls.Select(t => t.IsCompletedSuccessfully ? $"{t.Result}=ok" : "timeout"))}");

public record CartLine(string Sku, int Quantity, decimal UnitPrice)
{
    public decimal Total => Quantity * UnitPrice;
}
```

```text
1) 404.730 ₫
2) Xin chào, khách | Xin chào, khách | Xin chào, An
3) BOOK-001=3, PEN-002=1, MUG-003=1
4) CartLine { Sku = BOOK-001, Quantity = 3, UnitPrice = 149900, Total = 449700 } total=449700
5) success, client error, server error, other
6) Confirmed: 480.000 | Rejected: 60.000.000
7) 3, invalid, invalid
8) stock=ok, price=ok, timeout
```

Hai điều đáng chú ý trong output: `ToString` tự sinh của record in cả property tính toán `Total`; và ở bài 8,
`Task.WhenAll` ném `OperationCanceledException` nhưng hai Task đầu vẫn `IsCompletedSuccessfully`, nên lấy được kết quả từng phần.

</details>

---

## 13. Lỗi hay gặp

Các thông báo dưới đây là output thật của một project nháp cố tình gây lỗi (SDK 8.0.426).

| Triệu chứng | Nguyên nhân | Sửa |
|---|---|---|
| `warning CS8618: Non-nullable property 'Sku' must contain a non-null value when exiting constructor` | Nullable reference types bật mặc định: property `string` (không có `?`) có thể chưa được gán | `required`, gán trong constructor, hoặc `string?` nếu nghiệp vụ cho phép null ([§3](#3-null-an-toàn-dotnet-run----nulls)) |
| `warning CS8602: Dereference of a possibly null reference.`, chạy thì `NullReferenceException` | Dùng biến `string?` mà chưa kiểm null | `if (x is null) ...`, `x?.Length`, `x ?? ""` (§3); đừng dập cảnh báo bằng `!` cho xong |
| `warning CS4014: Because this call is not awaited, execution of the current method continues before the call is completed.` | Gọi method `async` mà quên `await`: exception bên trong bị nuốt, thứ tự chạy sai | Thêm `await` ([§10](#10-asyncawait-dotnet-run----async)) |
| `warning CS8509: The switch expression does not handle all possible values of its input type`, chạy thì `SwitchExpressionException: Non-exhaustive switch expression failed to match its input.` | `switch` expression thiếu trường hợp (ở đây `Status.Rejected`) | Thêm nhánh còn thiếu hoặc `_ => ...` ([§7](#7-pattern-matching-dotnet-run----patterns)) |
| `System.InvalidOperationException: Sequence contains no elements` | `First()` hoặc `Single()` trên dãy rỗng | `FirstOrDefault()` rồi kiểm null, hoặc chắc chắn dãy có phần tử ([§8](#8-linq-dotnet-run----linq)) |
| Lambda trong `Where` chạy hai lần (đo: `Count()` rồi `Any()` trên cùng một query, 1 phần tử, lambda chạy 2 lần) | LINQ **trì hoãn**: mỗi lần duyệt chạy lại cả query; với EF Core là **hai câu SQL** | `.ToList()` một lần rồi dùng list (§8) |
| `0.1 + 0.2` ra `0.30000000000000004` | `double` là số nhị phân xấp xỉ | Tiền dùng `decimal`: `0.1m + 0.2m` ra đúng `0.3` ([§2](#2-kiểu-dữ-liệu-số-chuỗi-dotnet-run----types)) |

---

## 14. Tự kiểm tra

- [ ] Giải thích được vì sao tiền dùng `decimal`, và vì sao định dạng số phải ghi culture.
- [ ] Đọc được `string?`, `?.`, `??`, `??=`, `is { } x`.
- [ ] Viết được class có primary constructor, property `private set`, record có `with`.
- [ ] Viết được `switch` biểu thức có pattern kiểu, property, so sánh.
- [ ] Viết được LINQ `Where/Select/GroupBy/OrderBy/First/ToDictionary` và giải thích thực thi trì hoãn.
- [ ] Biết khi nào dùng `TryParse` thay vì `try/catch`; dùng được exception filter `when`.
- [ ] Chạy song song bằng `Task.WhenAll`, huỷ bằng `CancellationToken`.

## 15. Ranh giới trung thực

- **Đã chạy thật** với .NET SDK 8.0.426 (C# 12): toàn bộ `CSharpBasics` và lời giải bài tập; output dán nguyên văn.
- Số mili giây ở phần async đã làm tròn tới 100 ms trong code; máy bạn chạy chậm hơn có thể ra `~400 ms`.
- Bài này chọn những gì dùng **ngay** ở bài 06–11. Chưa nói tới: `struct` tự viết, `Span<T>`, `IAsyncEnumerable`,
  `ref`/`unsafe`, source generator, `event`. Gặp trong dự án thì tra tài liệu Microsoft theo đúng tên đó.
