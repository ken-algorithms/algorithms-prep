using System.Globalization;

namespace CSharpBasics;

public static class Oop
{
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

    public static Task Patterns()
    {
        object?[] values = [null, -5, 42, "", "hi", new Money(2_000_000m, "VND"), new Money(5m, "USD"), new[] { 1, 2, 3 }, 3.14];
        foreach (var value in values)
            Console.WriteLine($"{Describe(value),-28} <- {value ?? "null"}");

        foreach (var total in new[] { 120_000m, 250_000m, 800_000m })
            Console.WriteLine($"shipping fee for {total.ToVnd()}: {ShippingFee(total).ToVnd()}");
        return Task.CompletedTask;

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
    }
}

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

public record Money(decimal Amount, string Currency); // mot dong = class bat bien + Equals + ToString + deconstruct

public static class MoneyExtensions
{
    // extension method: them method cho kieu co san (decimal) ma khong sua kieu do
    public static string ToVnd(this decimal amount) =>
        amount.ToString("N0", CultureInfo.GetCultureInfo("vi-VN")) + " ₫";
}
