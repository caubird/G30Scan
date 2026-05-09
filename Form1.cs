using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO;
using System.IO.Ports;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace TestG30
{
    public partial class MainForm : Form
    {
        // 串口对象
        private SerialPort _serialPort;

        String ReceiveBuf = "";
        public MainForm()
        {
            InitializeComponent();
            InitializeSerialPort();
            RefreshComPortList();
        }

        // 初始化串口对象
        private void InitializeSerialPort()
        {
            _serialPort = new SerialPort();
            // 设置默认串口参数，可根据需要修改
            _serialPort.BaudRate = 9600;
            _serialPort.DataBits = 8;
            _serialPort.StopBits = StopBits.One;
            _serialPort.Parity = Parity.None;

            // 绑定数据接收事件
            //_serialPort.DataReceived += SerialPort_DataReceived;
        }

        // 刷新串口列表
        private void RefreshComPortList()
        {
            // 保存当前选中项
            string currentPort = cboComPorts.SelectedItem?.ToString();

            // 清空列表
            cboComPorts.Items.Clear();

            // 获取所有可用串口并添加到下拉列表
            string[] ports = SerialPort.GetPortNames();
            foreach (string port in ports)
            {
                cboComPorts.Items.Add(port);
            }

            // 恢复选中状态
            if (!string.IsNullOrEmpty(currentPort) && cboComPorts.Items.Contains(currentPort))
            {
                cboComPorts.SelectedItem = currentPort;
            }
            else if (cboComPorts.Items.Count > 0)
            {
                cboComPorts.SelectedIndex = 0;
            }
        }

        // 串口数据接收事件
        private void SerialPort_DataReceived(object sender, SerialDataReceivedEventArgs e)
        {
            try
            {
                // 读取接收的数据
                string data = _serialPort.ReadExisting();

                // 跨线程更新UI
                if (txtReceivedData.InvokeRequired)
                {
                    txtReceivedData.Invoke(new Action<string>(AppendReceivedData), data);
                }
                else
                {
                    AppendReceivedData(data);
                }
            }
            catch (Exception ex)
            {
                MessageBox.Show($"接收数据出错: {ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        // 追加接收的数据到文本框
        private void AppendReceivedData(string data)
        {
            txtReceivedData.AppendText(data);
            if (txtReceivedData.Text.Length > 5000)
                txtReceivedData.Clear();
            ReceiveBuf += data;
            // 自动滚动到最后
            txtReceivedData.SelectionStart = txtReceivedData.TextLength;
            txtReceivedData.ScrollToCaret();
        }

        DateTime starttime = DateTime.Now;

        bool OpenComPortFlag = false;
        // 打开/关闭串口按钮点击事件
        private void btnOpenClose_Click(object sender, EventArgs e)
        {

            if (_serialPort.IsOpen)
            {
                // 关闭串口
                try
                {
                    OpenComPortFlag = false;
                    TotalNum = 0;
                    Thread.SpinWait(500);
                    //Thread.Sleep(500);
                    _serialPort.Close();
                    btnOpenClose.Text = "打开串口";
                    cboComPorts.Enabled = true;
                    btnRefresh.Enabled = true;
                    lblStatus.Text = "状态：已关闭";
                    lblStatus.ForeColor = System.Drawing.Color.Red;
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"关闭串口失败: {ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
            else
            {
                OpenComPortFlag = true;
                // 打开串口
                if (cboComPorts.SelectedItem == null)
                {
                    MessageBox.Show("请选择一个串口", "提示", MessageBoxButtons.OK, MessageBoxIcon.Information);
                    return;
                }

                try
                {
                    _serialPort.PortName = cboComPorts.SelectedItem.ToString();
                    _serialPort.BaudRate = 115200;
                    _serialPort.Open();
                    btnOpenClose.Text = "关闭串口";
                    cboComPorts.Enabled = false;
                    btnRefresh.Enabled = false;
                    lblStatus.Text = $"状态：已打开 {_serialPort.PortName}";
                    lblStatus.ForeColor = System.Drawing.Color.Green;

                    starttime = DateTime.Now;
                }
                catch (Exception ex)
                {
                    MessageBox.Show($"打开串口失败: {ex.Message}", "错误", MessageBoxButtons.OK, MessageBoxIcon.Error);
                }
            }
        }

        // 刷新串口列表按钮点击事件
        private void btnRefresh_Click(object sender, EventArgs e)
        {
            RefreshComPortList();
        }

        // 窗体关闭事件
        private void MainForm_FormClosing(object sender, FormClosingEventArgs e)
        {
            // 关闭串口
            if (_serialPort.IsOpen)
            {
                _serialPort.Close();
            }
        }

        private void button1_Click(object sender, EventArgs e)
        {
            if (_serialPort.IsOpen)
            {
                _serialPort.Write(textBox1.Text + "\r\n");
            }
        }

        private void MainForm_Load(object sender, EventArgs e)
        {

        }

        List<string> ReceivedLines = new List<string>();

        List<BeaconItem> Beacons = new List<BeaconItem>();

        long TotalNum = 0;
        private void button2_Click(object sender, EventArgs e)
        {
            try
            {
                string[] Lines = ReceiveBuf.Split("\r\n".ToCharArray());
                int index = ReceiveBuf.LastIndexOf("\r\n");
                ReceiveBuf = "";// ReceiveBuf.Remove(index + 1);
                for (int i = 0; i < Lines.Length; i++)
                {
                    //ReceivedLines.Add(Lines[i]);
                }

                this.Invoke(new MethodInvoker(delegate
                {
                    //SerialPort_DataReceived(null, null);


                    foreach (string line in Lines)
                    {
                        string[] strings = { ",", ":" };
                        string[] charactors = line.Split(strings, StringSplitOptions.None);
                        if (charactors.Length >= 4)
                        {
                            string chanel = charactors[2];
                            string RSSI = charactors[3];
                            string MAC = charactors[4];
                            if (MAC.Length == 12 && charactors.Length > 4)
                            {
                                string Length = charactors[5];
                                bool flag = false;
                                for (int i = 0; i < Beacons.Count; i++)
                                {
                                    if (Beacons[i].MAC == MAC)
                                    {
                                        Beacons[i].RSSI = (double)(Beacons[i].RSSI * Beacons[i].ReceiveCount + int.Parse(RSSI)) / (Beacons[i].ReceiveCount + 1);
                                        Beacons[i].ReceiveCount += 1;
                                        if (int.Parse(Length) > 0)
                                        {
                                            Beacons[i].ReceivedMSG.Add(charactors[6]);
                                            TotalNum++;
                                        }
                                        flag = true; break;
                                    }
                                }
                                if (!flag)
                                {
                                    BeaconItem newBeacon = new BeaconItem();
                                    newBeacon.MAC = MAC;
                                    newBeacon.RSSI = int.Parse(RSSI);//(double)(newBeacon.RSSI * Beacons[i].ReceiveCount + int.Parse(RSSI)) / (Beacons[i].ReceiveCount + 1);
                                    newBeacon.ReceiveCount += 1;
                                    if (int.Parse(Length) > 0)
                                    {
                                        newBeacon.ReceivedMSG.Add(charactors[6]);
                                        TotalNum++;
                                    }
                                    Beacons.Add(newBeacon);
                                }
                            }
                        }
                    }
                    if (sender != null && e != null)
                    {
                        this.Invoke(new MethodInvoker(delegate
                        {
                            numbuf = TotalNum;
                            listBox1.Items.Clear();
                            foreach (BeaconItem be in Beacons)
                            {
                                if (textBox2.Text.Length >= 2 && be.MAC.StartsWith(textBox2.Text))
                                    listBox1.Items.Add(be.MAC + ":" + be.RSSI.ToString("F0") + "@" + be.ReceiveCount.ToString());
                                else if (textBox2.Text.Length < 2)
                                    listBox1.Items.Add(be.MAC + ":" + be.RSSI.ToString("F0") + "@" + be.ReceiveCount.ToString());

                                label2.Text = listBox1.Items.Count.ToString();
                                label3.Text = "总数：" + TotalNum.ToString();
                            }
                        }));
                    }
                }));

            }
            catch
            {

            }
        }
        private void button3_Click(object sender, EventArgs e)
        {
            saveFileDialog1.Filter = "CSV文件(*.csv)|*.csv";
            saveFileDialog1.ShowDialog();

            //string Paht = 
            if (saveFileDialog1.CheckPathExists)
            {
                string FileName = saveFileDialog1.FileName;
                string buff = "MAC,ReceiveCount,RSSI\r\n";
                foreach (BeaconItem be in Beacons)
                {
                    if (be.MAC.StartsWith(textBox2.Text))
                        buff += be.MAC + "," + be.ReceiveCount + "," + be.RSSI.ToString("F2") + "\r\n";
                }
                File.AppendAllText(FileName, buff);
            }
            else
            {
                MessageBox.Show("目录不存在");
            }
        }
        long counter = 0;
        long numbuf = 0;
        private void timer1_Tick(object sender, EventArgs e)
        {
            if (_serialPort.IsOpen)
            {
                label1.Text = "开始：" + (DateTime.Now - starttime).TotalSeconds.ToString() + "s";
            }
             
            if (counter % 2 != 0)
            {
                this.Invoke(new MethodInvoker(delegate
                {
                    button2_Click(sender, e);
                }));
            }

            if (numbuf != TotalNum)
            {
                    this.Invoke(new MethodInvoker(delegate
                {
                    numbuf = TotalNum;
                    listBox1.Items.Clear();
                    foreach (BeaconItem be in Beacons)
                    {
                        if (textBox2.Text.Length >= 2 && be.MAC.StartsWith(textBox2.Text))
                            listBox1.Items.Add(be.MAC + ":" + be.RSSI.ToString("F0") + "@" + be.ReceiveCount.ToString());
                        else if (textBox2.Text.Length < 2)
                            listBox1.Items.Add(be.MAC + ":" + be.RSSI.ToString("F0") + "@" + be.ReceiveCount.ToString());


                        label2.Text = listBox1.Items.Count.ToString();
                        label3.Text = "总数：" + TotalNum.ToString();
                    }
                }));
            }



            counter++;
        }

        private void label2_Click(object sender, EventArgs e)
        {

        }

        private void timer2_Tick(object sender, EventArgs e)
        {
            if (OpenComPortFlag == true && _serialPort.IsOpen &&
                    _serialPort.BytesToRead > 0)
            {

                this.Invoke(new MethodInvoker(delegate
                {
                    SerialPort_DataReceived(null, null);
                }));

            }
        }
    }
    public class BeaconItem
    {
        public string MAC = "";
        public double RSSI = 0;
        public int ReceiveCount = 0;
        public List<string> ReceivedMSG = new List<string>();
    }
}
