using Microsoft.Win32;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using System.Windows;

namespace Rio3FlashPro.Desktop;

public partial class MainWindow : Window
{
    private string _lastReport = "Rio3FlashPro — отчёт\nАвтомобиль: Kia Rio III, 2016, 1.6 л, АКПП (по данным владельца)\nЭБУ: не идентифицирован\nПодключение: отсутствует\n";

    public MainWindow()
    {
        InitializeComponent();
    }

    private async void InspectFirmware_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog
        {
            Title = "Выберите файл для базовой проверки",
            Filter = "Файлы прошивок (*.bin;*.hex;*.mot)|*.bin;*.hex;*.mot|Все файлы (*.*)|*.*",
            CheckFileExists = true,
            Multiselect = false
        };

        if (dialog.ShowDialog(this) != true)
            return;

        StatusText.Text = "Проверка файла…";
        try
        {
            var path = dialog.FileName;
            var report = await Task.Run(() => InspectFile(path));
            _lastReport = report;
            OutputText.Text = report;
            StatusText.Text = "Файл проверен. Запись в ЭБУ не выполнялась.";
        }
        catch (Exception ex)
        {
            StatusText.Text = "Проверка не выполнена.";
            OutputText.Text = $"Ошибка: {ex.Message}";
        }
    }

    private static string InspectFile(string path)
    {
        var info = new FileInfo(path);
        if (!info.Exists)
            throw new FileNotFoundException("Файл не найден.", path);

        var extension = info.Extension.ToLowerInvariant();
        if (extension is not (".bin" or ".hex" or ".mot"))
            throw new InvalidDataException("Поддерживаются только BIN, HEX и MOT.");

        const long maxSize = 64L * 1024L * 1024L;
        if (info.Length is < 1 or > maxSize)
            throw new InvalidDataException("Размер файла должен быть от 1 байта до 64 МБ.");

        using var stream = File.OpenRead(path);
        var hash = Convert.ToHexString(SHA256.HashData(stream)).ToLowerInvariant();

        var sb = new StringBuilder();
        sb.AppendLine("Rio3FlashPro — базовая проверка файла");
        sb.AppendLine($"Имя: {info.Name}");
        sb.AppendLine($"Размер: {info.Length:N0} байт");
        sb.AppendLine($"Расширение: {extension}");
        sb.AppendLine($"SHA-256: {hash}");
        sb.AppendLine();
        sb.AppendLine("РЕЗУЛЬТАТ: файл прочитан, контрольная сумма вычислена.");
        sb.AppendLine("НЕ ПРОВЕРЕНО: формат производителя, контрольные суммы ЭБУ,");
        sb.AppendLine("совместимость с конкретным блоком, калибровка и безопасность записи.");
        sb.AppendLine("Запись в ЭБУ не выполнялась.");
        return sb.ToString();
    }

    private void SaveReport_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new SaveFileDialog
        {
            Title = "Сохранить отчёт Rio3FlashPro",
            FileName = $"Rio3FlashPro_Report_{DateTime.Now:yyyyMMdd_HHmmss}.txt",
            Filter = "Текстовый отчёт (*.txt)|*.txt"
        };

        if (dialog.ShowDialog(this) != true)
            return;

        try
        {
            File.WriteAllText(dialog.FileName, _lastReport, new UTF8Encoding(false));
            StatusText.Text = $"Отчёт сохранён: {dialog.FileName}";
        }
        catch (Exception ex)
        {
            StatusText.Text = $"Не удалось сохранить отчёт: {ex.Message}";
        }
    }
}
