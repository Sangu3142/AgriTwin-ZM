"""
AgriTwin-ZM Documentation Generator
Produces AgriTwin_ZM_Documentation.docx using python-docx.
Run: python generate_docs.py
"""

from docx import Document
from docx.shared import Pt, RGBColor, Cm, Inches, Twips
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
from docx.enum.section import WD_SECTION
import datetime
import copy

# ─── Palette ───────────────────────────────────────────────────────────────────
NAVY       = RGBColor(0x0F, 0x15, 0x35)
BLUE_H2    = RGBColor(0x1A, 0x2A, 0x6C)
DARK_TEXT  = RGBColor(0x2D, 0x2D, 0x2D)
GREY_H3    = RGBColor(0x33, 0x33, 0x33)
CYAN_ACC   = RGBColor(0x00, 0xB5, 0xD8)
WHITE      = RGBColor(0xFF, 0xFF, 0xFF)
CODE_BG    = "F4F4F4"
TBL_HDR    = "0F1535"
TBL_ALT    = "F2F7F2"
TBL_BRD    = "CCCCCC"
GREY_META  = RGBColor(0x71, 0x80, 0x96)

TODAY = "10 May 2026"

# ─── Low-level XML helpers ─────────────────────────────────────────────────────

def set_cell_bg(cell, hex_color):
    tc   = cell._tc
    tcPr = tc.get_or_add_tcPr()
    shd  = OxmlElement('w:shd')
    shd.set(qn('w:val'),   'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'),  hex_color)
    tcPr.append(shd)

def set_cell_border(cell, top=None, bottom=None, left=None, right=None):
    tc   = cell._tc
    tcPr = tc.get_or_add_tcPr()
    tcBorders = OxmlElement('w:tcBorders')
    for side, val in [('top',top),('bottom',bottom),('left',left),('right',right)]:
        if val:
            el = OxmlElement(f'w:{side}')
            el.set(qn('w:val'),   val.get('val','single'))
            el.set(qn('w:sz'),    val.get('sz','4'))
            el.set(qn('w:space'),'0')
            el.set(qn('w:color'), val.get('color', TBL_BRD))
            tcBorders.append(el)
    tcPr.append(tcBorders)

def set_table_borders(table):
    brd = {'val':'single','sz':'4','color': TBL_BRD}
    for row in table.rows:
        for cell in row.cells:
            set_cell_border(cell, top=brd, bottom=brd, left=brd, right=brd)

def add_para_border_bottom(para, hex_color="00B5D8", sz="12"):
    pPr  = para._p.get_or_add_pPr()
    pBdr = OxmlElement('w:pBdr')
    bot  = OxmlElement('w:bottom')
    bot.set(qn('w:val'),   'single')
    bot.set(qn('w:sz'),    sz)
    bot.set(qn('w:space'), '4')
    bot.set(qn('w:color'), hex_color)
    pBdr.append(bot)
    pPr.append(pBdr)

def add_page_break(doc):
    p  = OxmlElement('w:p')
    r  = OxmlElement('w:r')
    br = OxmlElement('w:br')
    br.set(qn('w:type'), 'page')
    r.append(br)
    p.append(r)
    doc.element.body.append(p)

def add_field(para, field_code):
    """Insert a Word field (PAGE, NUMPAGES) into a paragraph run."""
    fldChar1 = OxmlElement('w:fldChar')
    fldChar1.set(qn('w:fldCharType'), 'begin')
    instrText = OxmlElement('w:instrText')
    instrText.set(qn('xml:space'), 'preserve')
    instrText.text = field_code
    fldChar2 = OxmlElement('w:fldChar')
    fldChar2.set(qn('w:fldCharType'), 'separate')
    fldChar3 = OxmlElement('w:fldChar')
    fldChar3.set(qn('w:fldCharType'), 'end')
    run = para.add_run()
    run._r.append(fldChar1)
    run._r.append(instrText)
    run._r.append(fldChar2)
    run._r.append(fldChar3)
    return run

def set_line_spacing(para, spacing=1.3):
    pf = para.paragraph_format
    pf.line_spacing_rule = WD_LINE_SPACING.MULTIPLE
    pf.line_spacing      = spacing

def set_para_spacing(para, before=0, after=6):
    pf = para.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after  = Pt(after)

# ─── Document helpers ──────────────────────────────────────────────────────────

def body_run(para, text, bold=False, italic=False, color=None, size=11, font="Arial"):
    run = para.add_run(text)
    run.font.name  = font
    run.font.size  = Pt(size)
    run.font.bold  = bold
    run.font.italic = italic
    run.font.color.rgb = color if color else DARK_TEXT
    return run

def add_body(doc, text, bold=False, italic=False, color=None, size=11, align=WD_ALIGN_PARAGRAPH.JUSTIFY):
    para = doc.add_paragraph()
    para.alignment = align
    set_line_spacing(para, 1.3)
    set_para_spacing(para, 0, 6)
    body_run(para, text, bold=bold, italic=italic, color=color, size=size)
    return para

def add_h1(doc, text):
    add_page_break(doc)
    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.LEFT
    set_para_spacing(para, 12, 8)
    run = para.add_run(text)
    run.font.name  = "Arial"
    run.font.size  = Pt(18)
    run.font.bold  = True
    run.font.color.rgb = NAVY
    add_para_border_bottom(para, "00B5D8", "16")
    return para

def add_h2(doc, text):
    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.LEFT
    set_para_spacing(para, 10, 4)
    run = para.add_run(text)
    run.font.name  = "Arial"
    run.font.size  = Pt(14)
    run.font.bold  = True
    run.font.color.rgb = BLUE_H2
    return para

def add_h3(doc, text):
    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.LEFT
    set_para_spacing(para, 8, 3)
    run = para.add_run(text)
    run.font.name  = "Arial"
    run.font.size  = Pt(12)
    run.font.bold  = True
    run.font.color.rgb = GREY_H3
    return para

def add_bullet(doc, text, level=0, color=None):
    para = doc.add_paragraph(style='List Bullet')
    para.alignment = WD_ALIGN_PARAGRAPH.LEFT
    set_line_spacing(para, 1.3)
    set_para_spacing(para, 0, 3)
    run = para.add_run(text)
    run.font.name  = "Arial"
    run.font.size  = Pt(11)
    run.font.color.rgb = color if color else DARK_TEXT
    return para

def add_code_block(doc, text):
    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.LEFT
    pf = para.paragraph_format
    pf.left_indent  = Inches(0.5)
    pf.space_before = Pt(4)
    pf.space_after  = Pt(4)
    # background shading
    pPr = para._p.get_or_add_pPr()
    shd = OxmlElement('w:shd')
    shd.set(qn('w:val'),   'clear')
    shd.set(qn('w:color'), 'auto')
    shd.set(qn('w:fill'),  CODE_BG)
    pPr.append(shd)
    # border
    pBdr = OxmlElement('w:pBdr')
    for side in ['top','bottom','left','right']:
        el = OxmlElement(f'w:{side}')
        el.set(qn('w:val'),   'single')
        el.set(qn('w:sz'),    '4')
        el.set(qn('w:space'), '4' if side in ('top','bottom') else '72')
        el.set(qn('w:color'), 'CCCCCC')
        pBdr.append(el)
    pPr.append(pBdr)
    run = para.add_run(text)
    run.font.name  = "Courier New"
    run.font.size  = Pt(9)
    run.font.color.rgb = RGBColor(0x1A, 0x1A, 0x1A)
    return para

def make_table(doc, headers, rows, col_widths=None):
    """Create a styled table. headers = list of str, rows = list of list of str."""
    ncols = len(headers)
    table = doc.add_table(rows=1 + len(rows), cols=ncols)
    table.alignment = WD_TABLE_ALIGNMENT.LEFT
    table.style = 'Table Grid'

    # Header row
    hdr_row = table.rows[0]
    for i, h in enumerate(headers):
        cell = hdr_row.cells[i]
        set_cell_bg(cell, TBL_HDR)
        para = cell.paragraphs[0]
        para.alignment = WD_ALIGN_PARAGRAPH.LEFT
        run = para.add_run(h)
        run.font.name  = "Arial"
        run.font.size  = Pt(10)
        run.font.bold  = True
        run.font.color.rgb = WHITE

    # Data rows
    for ri, row_data in enumerate(rows):
        tr = table.rows[ri + 1]
        bg = TBL_ALT if ri % 2 == 1 else "FFFFFF"
        for ci, cell_text in enumerate(row_data):
            cell = tr.cells[ci]
            set_cell_bg(cell, bg)
            para = cell.paragraphs[0]
            para.alignment = WD_ALIGN_PARAGRAPH.LEFT
            run = para.add_run(str(cell_text) if cell_text else "")
            run.font.name  = "Arial"
            run.font.size  = Pt(10)
            run.font.color.rgb = DARK_TEXT
            set_line_spacing(para, 1.2)

    set_table_borders(table)

    # Column widths
    if col_widths:
        for ri2, row2 in enumerate(table.rows):
            for ci2, cell2 in enumerate(row2.cells):
                if ci2 < len(col_widths):
                    cell2.width = Cm(col_widths[ci2])
    doc.add_paragraph()
    return table

# ─── Page setup ────────────────────────────────────────────────────────────────

def setup_page(section):
    section.page_width    = Cm(21)
    section.page_height   = Cm(29.7)
    section.left_margin   = Cm(2.5)
    section.right_margin  = Cm(2.5)
    section.top_margin    = Cm(2.5)
    section.bottom_margin = Cm(2.5)

def add_header_footer(section):
    # Header
    header = section.header
    hpara  = header.paragraphs[0] if header.paragraphs else header.add_paragraph()
    hpara.clear()
    hpara.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    run = hpara.add_run("AgriTwin-ZM Documentation  ·  BIT 213/BSC 120")
    run.font.name = "Arial"
    run.font.size = Pt(9)
    run.font.color.rgb = GREY_META

    # Footer
    footer = section.footer
    fpara  = footer.paragraphs[0] if footer.paragraphs else footer.add_paragraph()
    fpara.clear()
    fpara.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run1 = fpara.add_run("Page ")
    run1.font.name = "Arial"; run1.font.size = Pt(9); run1.font.color.rgb = GREY_META
    add_field(fpara, " PAGE ")
    run2 = fpara.add_run(" of ")
    run2.font.name = "Arial"; run2.font.size = Pt(9); run2.font.color.rgb = GREY_META
    add_field(fpara, " NUMPAGES ")
    run3 = fpara.add_run("  ·  University of Lusaka")
    run3.font.name = "Arial"; run3.font.size = Pt(9); run3.font.color.rgb = GREY_META

# ─── COVER PAGE ────────────────────────────────────────────────────────────────

def build_cover(doc):
    sec = doc.sections[0]
    setup_page(sec)
    add_header_footer(sec)

    # Spacer
    for _ in range(4):
        p = doc.add_paragraph()
        set_para_spacing(p, 0, 0)

    # Title
    para = doc.add_paragraph()
    para.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run  = para.add_run("AgriTwin-ZM")
    run.font.name  = "Arial"
    run.font.bold  = True
    run.font.size  = Pt(40)
    run.font.color.rgb = NAVY
    set_para_spacing(para, 0, 6)

    # Subtitle
    para2 = doc.add_paragraph()
    para2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run2  = para2.add_run("Complete System Documentation")
    run2.font.name  = "Arial"
    run2.font.size  = Pt(18)
    run2.font.color.rgb = BLUE_H2
    run2.font.bold  = True
    set_para_spacing(para2, 0, 4)

    para3 = doc.add_paragraph()
    para3.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run3  = para3.add_run("User Guide  ·  Developer Reference")
    run3.font.name  = "Arial"
    run3.font.size  = Pt(14)
    run3.font.color.rgb = GREY_META
    set_para_spacing(para3, 0, 40)

    # Metadata table
    meta = [
        ["Project Name",   "AgriTwin-ZM"],
        ["Version",        "1.0"],
        ["Course",         "BIT 213 / BSC 120 — Introduction to Programming (Java)"],
        ["Institution",    "University of Lusaka"],
        ["Date Generated", TODAY],
        ["Authors",        "[Team Name]"],
    ]
    make_table(doc, ["Field", "Value"], meta, col_widths=[5, 11])

    add_page_break(doc)

# ─── TABLE OF CONTENTS ─────────────────────────────────────────────────────────

def build_toc(doc):
    add_h1.__wrapped__ = True   # skip page break for first real section after cover

    para = doc.add_paragraph()
    run  = para.add_run("Table of Contents")
    run.font.name  = "Arial"
    run.font.size  = Pt(18)
    run.font.bold  = True
    run.font.color.rgb = NAVY
    add_para_border_bottom(para, "00B5D8", "16")
    set_para_spacing(para, 0, 12)

    toc_entries = [
        ("1", "System Overview",                   ""),
        ("1.1", "What is AgriTwin-ZM?",            ""),
        ("1.2", "Key Features",                    ""),
        ("1.3", "System Requirements",             ""),
        ("2", "How to Install and Run",            ""),
        ("2.1", "Installation Steps",              ""),
        ("2.2", "Running for the First Time",      ""),
        ("2.3", "Troubleshooting Common Issues",   ""),
        ("3", "User Guide",                        ""),
        ("3.1", "Dashboard",                       ""),
        ("3.2", "Simulation Page",                 ""),
        ("3.3", "Storage Page",                    ""),
        ("3.4", "Policy Alerts Page",              ""),
        ("3.5", "Reports Page",                    ""),
        ("3.6", "Report Detail Page",              ""),
        ("3.7", "Information / Settings Page",     ""),
        ("4", "Project Structure",                 ""),
        ("4.1", "Folder and File Tree",            ""),
        ("4.2", "Package Structure",               ""),
        ("4.3", "Key Dependencies",                ""),
        ("5", "Class-by-Class Code Reference",     ""),
        ("5.1 – 5.26", "All 26 Java Classes",      ""),
        ("6", "Data Flow Diagrams",                ""),
        ("7", "Algorithms and Formulas",           ""),
        ("8", "File Formats",                      ""),
        ("9", "Testing Reference",                 ""),
        ("10", "Future Development Notes",         ""),
        ("11", "Glossary",                         ""),
    ]

    for num, title, _ in toc_entries:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        set_para_spacing(p, 0, 2)
        is_h1 = len(num) <= 2 and not num.endswith(".")
        indent = Pt(0) if (num.count('.') == 0 and '–' not in num) else Pt(16)
        p.paragraph_format.left_indent = indent
        run = p.add_run(f"{num}  {title}")
        run.font.name  = "Arial"
        run.font.size  = Pt(11 if not is_h1 else 12)
        run.font.bold  = is_h1
        run.font.color.rgb = NAVY if is_h1 else DARK_TEXT

    note = doc.add_paragraph()
    note.alignment = WD_ALIGN_PARAGRAPH.LEFT
    set_para_spacing(note, 12, 0)
    rn = note.add_run("Note: Open in Microsoft Word and press Ctrl+A then F9 to update page numbers.")
    rn.font.name   = "Arial"
    rn.font.size   = Pt(10)
    rn.font.italic = True
    rn.font.color.rgb = GREY_META

    add_page_break(doc)

# ─── SECTION 1 — SYSTEM OVERVIEW ───────────────────────────────────────────────

def build_section1(doc):
    add_h1(doc, "Section 1 — System Overview")

    add_h2(doc, "1.1  What is AgriTwin-ZM?")
    add_body(doc,
        "AgriTwin-ZM is a Java-based digital twin simulation platform designed to model the "
        "Zambian agricultural supply chain for two key commodities: maize and tomatoes. A digital "
        "twin is a software replica of a real-world system that allows decision-makers to explore "
        "'what-if' scenarios without affecting the physical world. AgriTwin-ZM lets a policy-maker, "
        "agricultural officer, or student enter the season's production figures, storage conditions, "
        "and market prices, then watch month-by-month how those inputs translate into surpluses or "
        "shortages, post-harvest losses, revenue, and food security indicators. The system also "
        "evaluates Zambia's Food Reserve Agency (FRA) policy rules automatically, alerting the user "
        "when national stock levels approach dangerous thresholds, when export restrictions should "
        "be considered, or when stored grain is at risk of aflatoxin contamination. The application "
        "runs entirely on a standard Java Development Kit — no internet connection or cloud service "
        "is required — making it accessible in rural and low-bandwidth environments."
    )

    add_h2(doc, "1.2  Key Features")
    features = [
        ("Maize Supply-Chain Simulation",
         "Simulates a multi-month maize season with compounded monthly storage loss, dynamic price "
         "adjustment, and monthly demand met from rolling stock. Moisture levels above 14% "
         "automatically increase the loss rate to model fungal growth."),
        ("Tomato Supply-Chain Simulation",
         "Models a single-period tomato run tracking daily spoilage. Cold storage cuts the "
         "effective spoilage rate by 40%, reflecting real-world cold-chain benefits. The system "
         "computes how much produce is saleable versus spoiled before reaching market."),
        ("Dynamic Price Elasticity",
         "When supply exceeds demand, the market price adjusts downward proportionally using a "
         "0.1 elasticity coefficient. When demand exceeds supply, prices rise. This mirrors "
         "real market behaviour and allows users to see price collapse scenarios."),
        ("Food Security Indicator (FSI)",
         "Calculates the percentage of demand covered by available production each period, "
         "capped at 100%. A value below 80% triggers a visual warning; below 60% signals a "
         "critical food security risk."),
        ("FRA Policy Engine",
         "Evaluates six rule-based government policies automatically: FRA strategic reserve "
         "threshold (500,000 t), export review trigger (surplus > 20% of production), maize "
         "storage time limit (6 months), tomato shelf-life check, aflatoxin risk detection "
         "(moisture > 14% for 3+ months), and farmer price-floor breach."),
        ("Seasonal Demand Modelling",
         "Monthly maize demand is adjusted using a sinusoidal model that peaks in July (dry "
         "season, when on-farm stocks are depleted) and troughs in January (harvest season). "
         "This reflects documented Zambian consumption patterns."),
        ("Persistent Report Saving",
         "After any simulation run, the user can save all records to a pipe-delimited .txt "
         "file in the reports/ folder. These files can be reloaded in a future session, "
         "shared, or opened directly in a text editor."),
        ("CSV Export",
         "Session results can be exported to a RFC 4180-compliant CSV file, suitable for "
         "import into Excel, Google Sheets, or statistical tools."),
        ("JavaFX Graphical Dashboard (Optional)",
         "An optional six-page dark-themed GUI provides stat cards with animated count-up "
         "displays, bar charts, line charts, pie charts, an area chart for Food Security, "
         "and a full interactive policy checker. The GUI is loaded reflectively so the "
         "console build always works without JavaFX."),
        ("Season Summary Report",
         "After multiple simulation months, a season summary aggregates total production, "
         "total waste, total revenue, average FSI, count of CRITICAL/WARNING alerts, and "
         "the top three most-frequent policy alert messages."),
        ("18-Test Test Suite",
         "A plain-main TestRunner (no JUnit) covers all core simulation logic: surplus, "
         "pricing, compounded loss, spoilage, FSI, FRA alerts, file round-trips, validation "
         "exceptions, seasonal demand, moisture-adjusted loss, aflatoxin detection, season "
         "summary aggregation, and CSV export."),
    ]
    for feat, desc in features:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        set_line_spacing(p, 1.3)
        set_para_spacing(p, 0, 4)
        body_run(p, feat + ": ", bold=True, color=NAVY)
        body_run(p, desc)

    add_h2(doc, "1.3  System Requirements")
    make_table(doc,
        ["Requirement", "Details"],
        [
            ["Java version",         "JDK 11 or newer (any vendor — OpenJDK, Temurin, Oracle)"],
            ["JavaFX version",       "JavaFX SDK 17+ (optional — required only for --gui mode)"],
            ["Operating system",     "Windows 10/11, macOS 11+, Ubuntu 20.04+ (any OS with JDK 11+)"],
            ["Minimum RAM",          "256 MB for console mode; 512 MB recommended for JavaFX GUI"],
            ["Disk space",           "Approximately 5 MB for compiled classes; < 1 MB source code"],
            ["Build tools",          "None — plain javac compiler included with any JDK"],
            ["PDFBox (bundled)",     "pdfbox-app-3.0.1.jar in lib/ — available for future PDF export"],
        ],
        col_widths=[5, 11]
    )

# ─── SECTION 2 — INSTALLATION ──────────────────────────────────────────────────

def build_section2(doc):
    add_h1(doc, "Section 2 — How to Install and Run")

    add_h2(doc, "2.1  Installation Steps")
    steps = [
        ("Install Java JDK 11+",
         "Download and install the Java Development Kit from https://adoptium.net (Eclipse "
         "Temurin is recommended). Verify the installation: open a terminal and run "
         "'javac -version'. You should see 'javac 11.x.x' or higher."),
        ("Download the Project",
         "Clone the repository with 'git clone <your-repo-url> AgriTwin-ZM' or download "
         "and extract the ZIP file. Navigate into the project folder: 'cd AgriTwin-ZM'."),
        ("Build (Console-Only — no JavaFX needed)",
         "On Windows, double-click build.bat or run it from the command prompt. On "
         "Linux/macOS, run:\n"
         "  mkdir -p out\n"
         "  find src -name '*.java' ! -name 'GuiApp.java' ! -name 'DashboardController.java' "
         "! -path '*/components/*' ! -path '*/ui/model/*' ! -path '*/SimulationPage.java' "
         "! -path '*/ReportsPage.java' ! -path '*/ReportDetailPage.java' > sources.txt\n"
         "  javac -d out @sources.txt"),
        ("Build with JavaFX GUI (optional)",
         "Download JavaFX SDK 17+ from https://gluonhq.com/products/javafx/ and extract it. "
         "Set the environment variable JAVAFX_LIB to the lib subfolder, then run "
         "build_with_javafx.bat on Windows, or:\n"
         "  export JAVAFX_LIB=/path/to/javafx-sdk/lib\n"
         "  javac --module-path \"$JAVAFX_LIB\" --add-modules javafx.controls -d out "
         "$(find src -name '*.java')"),
        ("Run the Application (Console)",
         "  java -cp out agritwin.Main"),
        ("Run the Application (GUI)",
         "  java --module-path \"%JAVAFX_LIB%\" --add-modules javafx.controls "
         "-cp out agritwin.Main --gui"),
        ("Run the Test Suite",
         "  java -cp out agritwin.TestRunner\n"
         "All 18 tests should report [PASS]."),
    ]
    for i, (title, detail) in enumerate(steps, 1):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
        set_line_spacing(p, 1.3)
        set_para_spacing(p, 4, 3)
        body_run(p, f"Step {i}: {title}. ", bold=True, color=NAVY)
        body_run(p, detail)

    add_h2(doc, "2.2  Running for the First Time")
    add_body(doc,
        "When launched in console mode, the application prints an ASCII banner reading "
        "'AgriTwin-ZM v1.0 — Digital Twin for Zambian Agriculture' followed by a numbered "
        "main menu with seven options: Run Maize Simulation, Run Tomato Simulation, View "
        "Reports, Load Previous Simulation, Save Current Simulation, Exit, and Export CSV. "
        "Each option prompts for parameter values; pressing Enter accepts the bracketed "
        "default. When launched with --gui, a 1280x720 window opens showing a dark navy "
        "dashboard with a sidebar on the left (Dashboard, Simulation, Storage, Policy Alerts, "
        "Reports, Information) and a top bar with a search field. The Dashboard page shows "
        "five stat cards (Production Volume, Market Demand, Net Surplus, Est. Revenue, Food "
        "Security) populated with dashes until the first simulation is run."
    )

    add_h2(doc, "2.3  Troubleshooting Common Issues")
    make_table(doc,
        ["Problem", "Likely Cause", "Solution"],
        [
            ["Application does not launch",
             "JDK not installed or not on PATH",
             "Run 'java -version' to verify; reinstall JDK and ensure JAVA_HOME is set"],
            ["JavaFX modules not found error",
             "--module-path or --add-modules missing from run command",
             "Use the exact run command from BUILD.txt; ensure JAVAFX_LIB points to the lib folder"],
            ["'javac: command not found'",
             "JDK bin directory not on PATH",
             "Add the JDK bin directory to your system PATH environment variable"],
            ["Blank/white window appears",
             "JavaFX cannot render on this display (e.g. no GPU driver)",
             "Try setting -Dprism.order=sw in the run command to force software rendering"],
            ["reports/ folder not created",
             "Save button was never clicked",
             "The reports/ directory is created automatically on first Save; no manual setup needed"],
            ["build.bat: compilation FAILED",
             "Source file error or wrong JDK version",
             "Check the error message; ensure JDK version is 11+; re-read all .java files for syntax issues"],
        ],
        col_widths=[5, 5, 6]
    )

# ─── SECTION 3 — USER GUIDE ────────────────────────────────────────────────────

def build_section3(doc):
    add_h1(doc, "Section 3 — User Guide")
    add_body(doc,
        "This section describes every page of the JavaFX graphical interface. Console-mode "
        "users will find equivalent functionality through the numbered menu options described "
        "in brackets after each feature."
    )

    # 3.1 Dashboard
    add_h2(doc, "3.1  Dashboard")
    add_body(doc,
        "The Dashboard is the home screen of the application, visible immediately after "
        "launch. It provides a high-level summary of the most recent simulation run, "
        "including five key performance metrics and a live policy alert panel. Before any "
        "simulation is run, all metric cards display a dash placeholder."
    )
    make_table(doc,
        ["Element", "Type", "What it does", "When to use"],
        [
            ["Production Volume card", "Stat card",    "Displays total production in tons with an animated progress bar", "Check after running a simulation to see initial stock"],
            ["Market Demand card",     "Stat card",    "Shows total projected demand in tons",                           "Verify demand input was recorded correctly"],
            ["Net Surplus card",       "Stat card",    "Shows positive surplus or negative shortage",                    "Identify over/under-supply at a glance"],
            ["Est. Revenue card",      "Stat card",    "Displays total estimated revenue in ZMW (K/M/B formatted)",      "Gauge economic outcome of the season"],
            ["Food Security card",     "Stat card",    "Shows average FSI percentage across all simulation months",      "Red < 60%, yellow 60–79%, green >= 80%"],
            ["Recent Simulations list","List panel",   "Shows up to 3 recent runs with commodity icon, FSI and revenue", "Review recent runs without navigating to Reports"],
            ["FRA Policy Status panel","Alert panel",  "Shows concatenated policy alert messages from the last run",     "Quickly spot FRA reserve or export alerts"],
            ["View Report button",     "Button",       "Navigates to the Policy Alerts page",                           "Click for the full structured policy breakdown"],
            ["Clear button (x)",       "Button",       "Clears recent simulation history and resets stat cards to dashes","Reset the dashboard between sessions"],
            ["Run Simulation button",  "Button",       "Navigates directly to the Simulation page",                     "Click to start a new simulation"],
        ],
        col_widths=[4, 3, 5.5, 3.5]
    )
    add_body(doc,
        "How to view your latest results: After running a simulation, return to Dashboard "
        "by clicking 'Dashboard' in the sidebar. The five stat cards update automatically "
        "with the new results, and the Recent Simulations list shows a new entry at the top.",
        italic=True
    )

    # 3.2 Simulation Page
    add_h2(doc, "3.2  Simulation Page")
    add_body(doc,
        "The Simulation page is where you configure and run a supply-chain scenario. Select "
        "Maize or Tomato using the tabs at the top. The form shows shared fields (Production "
        "Volume, Market Demand, Price) and commodity-specific fields below. Click Run "
        "Simulation to compute results, which appear as stat cards, two charts, a pie chart, "
        "and a policy text area below the form."
    )
    make_table(doc,
        ["Element", "Type", "What it does", "When to use"],
        [
            ["Maize / Tomato tab",         "Tab",       "Switches between commodity input modes; clears fields on switch",  "Select before entering parameters"],
            ["Production Volume (t)",      "Text field","Enter the total tons produced this season",                        "Required for both commodities"],
            ["Market Demand (t)",          "Text field","Enter the expected total demand in tons",                          "Required for both commodities"],
            ["Price per Ton / Crate (ZMW)","Text field","Farm-gate price per ton (Maize) or market price per crate (Tomato)","Required; drives revenue calculation"],
            ["Months to Simulate",         "Text field","Maize only: number of months to project (e.g. 6)",                "Sets simulation horizon for maize"],
            ["Monthly Loss % (storage)",   "Text field","Maize only: percentage lost per month in storage (e.g. 3.0)",      "Higher values model poor storage conditions"],
            ["Moisture Level %",           "Text field","Maize only: grain moisture percentage (above 14% raises loss rate)","Enter measured or estimated moisture"],
            ["Spoilage Rate (%/day)",       "Text field","Tomato only: percentage of batch spoiled per day",                "Reflects local temperature/handling conditions"],
            ["Shelf Life (days)",          "Text field","Tomato only: expected days until spoilage onset",                  "Use crop-specific shelf-life data"],
            ["Days Before Sale",           "Text field","Tomato only: days from harvest to market",                        "Affects total spoilage calculated"],
            ["Storage Type",               "Combo box", "Tomato only: COLD (40% spoilage reduction) or OPEN",              "Select based on available infrastructure"],
            ["Run Simulation button",      "Button",    "Validates inputs and runs the simulation; shows results section",  "Click after filling all required fields"],
            ["Clear Results button",       "Button",    "Hides results section and clears all input fields",                "Start a fresh simulation run"],
            ["Surplus/Shortage card",      "Stat card", "Net difference between available stock and demand",                "Positive = surplus; negative = shortage"],
            ["Adjusted Price card",        "Stat card", "Market price after elasticity adjustment",                        "Compare with input price to see price impact"],
            ["Food Security % card",       "Stat card", "Percentage of demand covered by available production",            "< 80% warrants attention"],
            ["Total Waste card",           "Stat card", "Tonnes lost to storage decay or spoilage",                       "Higher value indicates storage inefficiency"],
            ["Est. Revenue card",          "Stat card", "Total ZMW earned from the simulated sold volume",                 "Primary profitability indicator"],
            ["Monthly Stock vs Demand bar chart","Chart","Bar chart comparing remaining stock vs monthly demand per month", "Visual check for shortage months"],
            ["Stock Loss Schedule line chart","Chart",  "Line chart showing compounded stock remaining over time",         "Steeper slope = higher storage loss rate"],
            ["Policy Alert pie chart",     "Chart",     "Distribution of CRITICAL / WARNING / INFO alerts across months",  "Large CRITICAL slice = urgent policy action needed"],
            ["Policy text area",           "Text area", "Full PolicyEngine evaluation + season summary in plain text",     "Read for detailed per-rule policy status"],
            ["Save Report button",         "Button",    "Appears after simulation; opens dialog to name and save the run", "Click to persist results to the reports/ folder"],
        ],
        col_widths=[4.5, 2.5, 5, 4]
    )
    add_body(doc,
        "Important: The Save Report button only appears after a successful simulation run. "
        "If you clear results or switch commodity tabs, the button is hidden until the next run.",
        italic=True, color=RGBColor(0xF5, 0x65, 0x65)
    )

    # 3.3 Storage Page
    add_h2(doc, "3.3  Storage Page")
    add_body(doc,
        "The Storage page shows a dedicated analysis of post-harvest loss for the most "
        "recently completed simulation. Before any simulation is run, a placeholder icon "
        "and message are displayed. After a run, three stat cards show Initial Stock, "
        "Total Loss, and Remaining Stock, and a line chart shows how stock and cumulative "
        "loss evolve over the simulation period."
    )
    make_table(doc,
        ["Element", "Type", "What it does", "When to use"],
        [
            ["Initial Stock card",       "Stat card",  "Estimated starting tonnage (production + first-month waste reversed)", "Reference point for loss calculations"],
            ["Total Loss card",          "Stat card",  "Sum of all storage losses across every simulation period",             "Compare against initial stock for loss fraction"],
            ["Remaining Stock card",     "Stat card",  "Final stock tonnage at the end of the last period",                   "Indicates ending position entering next season"],
            ["Remaining Stock line",     "Line chart", "Stock trajectory from period 0 to final period",                      "Steeper descent = higher loss rate"],
            ["Cumulative Loss line",     "Line chart", "Running total of tonnage lost to decay/spoilage over time",           "Should plateau if loss rate drops over time"],
            ["Clear Figures button",     "Button",     "Resets all cards and hides the chart, returning to placeholder",      "Use when switching context between simulations"],
        ],
        col_widths=[4.5, 2.5, 5, 4]
    )

    # 3.4 Policy Alerts Page
    add_h2(doc, "3.4  Policy Alerts Page")
    add_body(doc,
        "The Policy Alerts page provides an interactive policy checker independent of the "
        "Simulation page. You can enter stock levels, surplus, and storage durations directly "
        "and evaluate all six FRA policy rules in real time. Results are displayed in a text "
        "area with severity labels. The page also shows alerts automatically from the most "
        "recent simulation run."
    )
    make_table(doc,
        ["Element", "Type", "What it does", "When to use"],
        [
            ["National Stock (t) field",  "Text field","Enter total national maize stock in tons",                      "Test FRA reserve threshold scenarios"],
            ["Surplus (t) field",         "Text field","Enter current surplus amount in tons",                          "Test export restriction trigger"],
            ["Production (t) field",      "Text field","Enter total production figure",                                 "Required denominator for export and FSI checks"],
            ["Months Stored field",       "Text field","How long maize has been stored",                                "Test storage time warning (> 6 months triggers alert)"],
            ["Days Stored field",         "Text field","Days tomato has been stored (-1 to skip)",                      "Test tomato shelf-life check"],
            ["Shelf Life field",          "Text field","Tomato shelf life in days (-1 to skip)",                        "Paired with Days Stored for shelf-life rule"],
            ["Check Policies button",     "Button",    "Evaluates all rules and writes structured results to text area", "Click after entering values"],
            ["Results text area",         "Text area", "Displays [CRITICAL]/[WARNING]/[INFO] labelled alert output",    "Read to understand policy status at given inputs"],
        ],
        col_widths=[4.5, 2.5, 5, 4]
    )

    # 3.5 Reports Page
    add_h2(doc, "3.5  Reports Page")
    add_body(doc,
        "The Reports page shows a card grid of all simulation reports saved to the reports/ "
        "folder. Each card displays the report name, date saved, commodity type, total "
        "revenue, number of months, average FSI, and FRA reserve status. Clicking a card "
        "opens the Report Detail view."
    )
    make_table(doc,
        ["Element", "Type", "What it does", "When to use"],
        [
            ["Report cards (grid)", "Card grid",  "One card per saved .txt file in reports/; auto-refreshes when new reports are saved", "Browse saved simulations"],
            ["Commodity avatar",    "Circle icon","Green circle = Maize; Red circle = Tomato; helps identify commodity at a glance",       "Visual identification"],
            ["FRA status row",      "Label",      "Shows green tick (FRA OK) or red warning (RESERVE ALERT)",                             "Quick policy health check"],
            ["FSI progress bar",    "Progress bar","Visual representation of average Food Security Indicator",                             "Compare FSI across reports"],
            ["View Report link",    "Clickable",  "Left-click card to open the Report Detail page for that report",                       "Read full simulation data"],
            ["Right-click menu",    "Context menu","Options: Delete Report (with confirmation dialog) or Copy File Path",                  "Manage saved files"],
            ["Go to Simulation btn","Button",     "Navigates to the Simulation page",                                                      "Quickly run a new simulation"],
            ["Refresh button (🔄)", "Button",     "Re-scans the reports/ folder and reloads the list",                                    "Use if reports were added outside the app"],
            ["Empty state",         "Placeholder","Shown when no reports exist; includes a Go to Simulation link",                         "Prompt to run first simulation"],
        ],
        col_widths=[4, 2.5, 6, 3.5]
    )

    # 3.6 Report Detail Page
    add_h2(doc, "3.6  Report Detail Page")
    add_body(doc,
        "Opened by clicking any report card. Shows the full simulation data for one saved "
        "report: a header with the report name, commodity, and save date; an FRA Policy "
        "Status card listing all unique policy alerts with colour-coded severity icons; two "
        "side-by-side charts (Surplus/Shortage trend and Revenue per month); an area chart "
        "for the Food Security Indicator with 80% and 60% reference lines; and a full data "
        "table with all simulation fields. All charts animate in with a fade transition."
    )
    make_table(doc,
        ["Element", "Type", "What it does"],
        [
            ["Back button (← Reports)",   "Button",      "Returns to the Reports grid, restoring the previous scroll position"],
            ["FRA Policy Status card",     "Card",        "Lists every unique alert from the simulation with colour coding: green = OK, red = CRITICAL, gold = WARNING"],
            ["Surplus/Shortage line chart","Line chart",  "Cyan line = surplus months; red line = shortage months; dashed grey = zero baseline"],
            ["Revenue bar chart",          "Bar chart",   "One blue bar per month showing ZMW revenue with tooltip on hover"],
            ["Food Security area chart",   "Area chart",  "FSI percentage per month; dashed gold line at 80% target; dashed red line at 60% critical threshold"],
            ["Full Simulation Data table", "Table",       "All 9 fields per record: Month, Commodity, Production, Demand, Surplus, Waste, Revenue, FSI (colour-coded), Policy Alert"],
        ],
        col_widths=[5, 3, 8]
    )

    # 3.7 Information/Settings Page
    add_h2(doc, "3.7  Information / Settings Page")
    add_body(doc,
        "Accessed via the 'Information' sidebar item or the (i) button in the top bar. "
        "This page does not contain user-configurable settings. Instead it is a reference "
        "panel describing what AgriTwin-ZM is, its academic context, a quick How-to-Use "
        "guide, and the key model parameters (thresholds, coefficients, and targets) for "
        "both the Maize and Tomato models."
    )
    make_table(doc,
        ["Card", "Contents"],
        [
            ["What is AgriTwin-ZM?", "Plain-English description of the digital twin concept and what the application calculates"],
            ["Academic Context",     "Course (BIT 213/BSC 120), institution (University of Lusaka), presentation date (13 May 2026), version, platform"],
            ["How to Use",           "Six-step quick-start: Simulate → Enter → Run → Review → Save → Dashboard"],
            ["Maize Model parameters","FRA Reserve 500,000 t; export trigger 20%; max storage 6 months; moisture threshold 14%; loss cap 20%/month; price elasticity 0.1"],
            ["Tomato Model parameters","Cold storage factor 0.6x; shelf-life warning; spoilage % per day; FSI green/warning/critical thresholds; revenue formula"],
        ],
        col_widths=[5, 11]
    )

# ─── SECTION 4 — PROJECT STRUCTURE ─────────────────────────────────────────────

def build_section4(doc):
    add_h1(doc, "Section 4 — Project Structure")

    add_h2(doc, "4.1  Folder and File Tree")
    add_code_block(doc,
"""AgriTwin-ZM/
├── src/
│   └── agritwin/
│       ├── Main.java                    <- entry point (console default, --gui reflective)
│       ├── TestRunner.java              <- 18 PASS/FAIL plain-main unit tests
│       ├── model/
│       │   ├── Commodity.java           <- abstract superclass (production, price, value)
│       │   ├── Maize.java               <- maize POJO (moisture, storage cap, loss rate)
│       │   ├── Tomato.java              <- tomato POJO (shelf life, storage type, spoilage)
│       │   ├── SimulationRecord.java    <- per-period snapshot DTO (9 fields + serialisation)
│       │   └── StorageType.java         <- COLD / OPEN enum (type-safe storage flag)
│       ├── service/
│       │   ├── SupplyDemandEngine.java  <- surplus, price elasticity, FSI, seasonal demand
│       │   ├── StorageLossSimulator.java<- compound maize decay, tomato spoilage, schedules
│       │   ├── PolicyEngine.java        <- FRA/export/storage/aflatoxin/price-floor rules
│       │   ├── ReportGenerator.java     <- console report, .txt export, .csv export
│       │   ├── SimulationRunner.java    <- month-by-month simulation state machine
│       │   └── Severity.java            <- INFO / WARNING / CRITICAL enum
│       ├── util/
│       │   ├── RecordStore.java         <- persistence interface (DI seam)
│       │   └── FileHandler.java         <- pipe-delimited save/load, implements RecordStore
│       └── ui/
│           ├── ConsoleUI.java           <- Scanner menu, delegates to SimulationRunner
│           ├── GuiApp.java              <- JavaFX Application bootstrap (optional)
│           ├── DashboardController.java <- 6-page JavaFX scene builder + navigation
│           ├── SimulationPage.java      <- Save Report logic and badge animation
│           ├── ReportsPage.java         <- report card grid, disk refresh, delete flow
│           ├── ReportDetailPage.java    <- 3-chart + table detail view for one report
│           └── components/
│               ├── StatCard.java        <- animated metric card with progress bar
│               ├── AlertPanel.java      <- FRA policy status panel with gold border
│               ├── SidebarMenu.java     <- dark sidebar navigation with hover effects
│               ├── ReportCard.java      <- clickable report thumbnail with context menu
│               └── (ui/model/)
│                   └── ReportSummary.java <- immutable DTO for Reports page grid
├── out/                                 <- compiled .class files (generated by build)
├── reports/                             <- saved simulation .txt files (created at runtime)
├── lib/
│   └── pdfbox-app-3.0.1.jar            <- bundled for future PDF export feature
├── docs/
│   ├── UML_ClassDiagram.puml            <- PlantUML source for class diagram
│   └── UML_ClassDiagram.txt             <- ASCII text fallback diagram
├── build.bat                            <- Windows console-only build script
├── build_with_javafx.bat                <- Windows full build with JavaFX
├── BUILD.txt                            <- plain-text build + run instructions
├── simulation.dat                       <- sample saved simulation file
└── sources_nogui.txt                    <- source file list for console build"""
    )

    add_body(doc, "Top-level folder purposes:")
    folder_desc = [
        ("src/", "All Java source code. Organised into packages that mirror the MVC + Service Layer architecture."),
        ("out/", "Compiled .class files output by javac -d out. Not committed to version control; regenerated by build.bat."),
        ("reports/", "Runtime-created directory. Every 'Save Report' click writes a pipe-delimited .txt file here."),
        ("lib/", "Third-party JARs. Currently contains pdfbox-app-3.0.1.jar bundled for a planned PDF export feature."),
        ("docs/", "Design artefacts: UML diagram in PlantUML (.puml) and plain-text (.txt) formats."),
    ]
    for folder, desc in folder_desc:
        p = doc.add_paragraph()
        set_line_spacing(p, 1.3); set_para_spacing(p, 0, 3)
        body_run(p, folder, bold=True, color=NAVY)
        body_run(p, " — " + desc)

    add_h2(doc, "4.2  Package Structure")
    make_table(doc,
        ["Package", "Classes", "Purpose"],
        [
            ["agritwin",             "Main, TestRunner",
             "Entry point and test suite. Main routes to ConsoleUI or GuiApp (reflective)."],
            ["agritwin.model",       "Commodity, Maize, Tomato, SimulationRecord, StorageType",
             "Data model layer. Plain-old-Java-objects with validated setters. No imports from service or UI layers."],
            ["agritwin.service",     "SupplyDemandEngine, StorageLossSimulator, PolicyEngine, ReportGenerator, SimulationRunner, Severity",
             "Business logic layer. All simulation algorithms, policy rules, and report formatting live here."],
            ["agritwin.util",        "RecordStore, FileHandler",
             "Persistence layer. RecordStore is the interface; FileHandler is the flat-file implementation."],
            ["agritwin.ui",          "ConsoleUI, GuiApp, DashboardController, SimulationPage, ReportsPage, ReportDetailPage",
             "Presentation layer. ConsoleUI needs no JavaFX; the rest require JavaFX SDK."],
            ["agritwin.ui.components","StatCard, AlertPanel, SidebarMenu, ReportCard",
             "Reusable JavaFX UI widgets. Each is a self-contained VBox/StackPane subclass."],
            ["agritwin.ui.model",    "ReportSummary",
             "UI-layer data transfer object. Carries pre-computed summary fields for the Reports grid."],
        ],
        col_widths=[4, 5, 7]
    )

    add_h2(doc, "4.3  Key Dependencies")
    make_table(doc,
        ["Library / Tool", "Version", "Purpose", "Where Used"],
        [
            ["Java Development Kit", "11+",   "Core language runtime, Collections, I/O, Logging, Reflection", "All .java files"],
            ["JavaFX",               "17+",   "Graphical user interface: Scene graph, Charts, Controls, Animations", "agritwin.ui.* (optional)"],
            ["PDFBox",               "3.0.1", "PDF document creation (bundled in lib/ for planned future PDF export)", "lib/pdfbox-app-3.0.1.jar — not yet called from source"],
            ["PlantUML",             "N/A",   "UML class diagram generation from docs/UML_ClassDiagram.puml (design tool, not runtime)", "docs/ only"],
        ],
        col_widths=[4, 2.5, 7, 2.5]
    )
    add_body(doc,
        "No Maven, Gradle, or JUnit dependencies exist. The build uses only javac "
        "and the standard Java library, fulfilling the BIT 213 course requirement for a "
        "vanilla-JDK submission.",
        italic=True
    )

# ─── SECTION 5 HELPER ─────────────────────────────────────────────────────────

def class_section(doc, number, class_name, package, description,
                  oop_concepts, fields, methods, notes=""):
    add_h2(doc, f"{number}  {class_name}")
    add_body(doc, description)
    p = doc.add_paragraph()
    set_line_spacing(p, 1.3); set_para_spacing(p, 0, 4)
    body_run(p, "Package: ", bold=True, color=NAVY)
    body_run(p, package)
    p2 = doc.add_paragraph()
    set_line_spacing(p2, 1.3); set_para_spacing(p2, 0, 4)
    body_run(p2, "OOP Concepts Used: ", bold=True, color=NAVY)
    body_run(p2, oop_concepts)
    if fields:
        add_h3(doc, "Key Fields")
        make_table(doc, ["Field Name", "Type", "Purpose"], fields, col_widths=[4.5, 3, 8.5])
    if methods:
        add_h3(doc, "Key Methods")
        make_table(doc, ["Method / Signature", "Returns", "Purpose"], methods, col_widths=[6, 2.5, 7.5])
    if notes:
        add_body(doc, notes, italic=True)

# ─── SECTION 5 — CLASS-BY-CLASS CODE REFERENCE ────────────────────────────────

def build_section5(doc):
    add_h1(doc, "Section 5 — Class-by-Class Code Reference")
    add_body(doc,
        "This section documents every Java class and enum in AgriTwin-ZM. For each class, "
        "the package location, a description, the OOP concepts it demonstrates, its key "
        "fields, and its key methods are listed. Omitted from the field/method tables are "
        "trivial getters/setters that follow standard JavaBean naming with no logic."
    )

    # 5.1 Main
    class_section(doc, "5.1", "Main.java", "agritwin",
        "Application entry point. Inspects command-line arguments; if --gui is present it "
        "loads GuiApp reflectively so the JavaFX dependency stays optional at compile time. "
        "Otherwise it instantiates ConsoleUI directly.",
        "Composition (delegates to ConsoleUI or GuiApp); Reflection (loads GuiApp without a "
        "compile-time JavaFX import, keeping the console build self-contained).",
        [],
        [
            ["main(String[] args)", "void",
             "Checks for --gui flag; delegates to launchGui() or new ConsoleUI().start()"],
            ["hasFlag(String[] args, String flag)", "boolean",
             "Case-insensitive linear scan of args array; returns true if flag is found"],
            ["launchGui(String[] args)", "void",
             "Loads agritwin.ui.GuiApp by name via Class.forName(); invokes launchApp(String[]). "
             "Catches ClassNotFoundException, NoClassDefFoundError, and Exception — each falls "
             "back to ConsoleUI with an error message on stderr"],
        ],
        "Main is declared final with a private constructor — it is a pure utility class with "
        "no mutable state of its own."
    )

    # 5.2 Commodity
    class_section(doc, "5.2", "Commodity.java", "agritwin.model",
        "Abstract superclass for all agricultural commodities. Encapsulates the two fields "
        "common to every commodity (productionVolume and price) and declares two abstract "
        "methods that each subclass must implement. The validated setter for productionVolume "
        "enforces a non-negative invariant across all subclasses.",
        "Abstraction (abstract class and abstract methods); Encapsulation (protected fields "
        "with validated setter); Inheritance (Maize and Tomato extend this class).",
        [
            ["productionVolume", "double (protected)",
             "Tonnes of the commodity produced in the current period"],
            ["price", "double (protected)",
             "Current market price per unit (ZMW per tonne for maize; derived for tomato)"],
        ],
        [
            ["getProductionVolume()", "double", "Returns current production volume"],
            ["setProductionVolume(double v)", "void",
             "Validates v >= 0; throws IllegalArgumentException if negative"],
            ["getPrice() / setPrice(double p)", "double / void", "Standard accessor pair for price"],
            ["estimateTotalValue()", "double (abstract)",
             "Subclass-specific total monetary value calculation"],
            ["getCommodityName()", "String (abstract)",
             "Returns the human-readable name of the commodity"],
        ]
    )

    # 5.3 Maize
    class_section(doc, "5.3", "Maize.java", "agritwin.model",
        "Concrete Commodity subclass representing maize. The constructor validates all five "
        "parameters: productionVolume >= 0, price >= 0, storageCapacity > 0, moistureLevel "
        "in [0,100], and lossPercentagePerMonth in [0,100]. Moisture levels above 14% are "
        "later used by StorageLossSimulator to increase the effective loss rate.",
        "Inheritance (extends Commodity); Encapsulation (private fields with getters/setters); "
        "Polymorphism (overrides estimateTotalValue() and getCommodityName()).",
        [
            ["storageCapacity", "double", "Maximum tonnes that can be held in store"],
            ["moistureLevel",   "double", "Moisture content as a percentage (0-100); >14% raises loss rate"],
            ["lossPercentagePerMonth", "double",
             "Base monthly loss rate as a percentage (0-100) before moisture adjustment"],
        ],
        [
            ["Maize(double prod, double price, double cap, double moist, double loss)",
             "constructor", "Validates all 5 parameters; throws IllegalArgumentException on any violation"],
            ["estimateTotalValue()", "double", "Returns productionVolume * price"],
            ["getCommodityName()", "String", "Returns \"Maize\""],
        ]
    )

    # 5.4 Tomato
    class_section(doc, "5.4", "Tomato.java", "agritwin.model",
        "Concrete Commodity subclass for tomatoes. The market price is derived from "
        "marketPricePerCrate * 40 (crates per tonne), stored in the inherited price field. "
        "The StorageType enum controls which spoilage factor StorageLossSimulator applies.",
        "Inheritance (extends Commodity); Encapsulation; Polymorphism (overrides "
        "estimateTotalValue, getCommodityName, getPricePerTon); Enum usage (StorageType).",
        [
            ["shelfLifeDays",       "int",         "Maximum saleable life in days under ideal conditions"],
            ["storageType",         "StorageType", "COLD or OPEN — determines spoilage factor applied"],
            ["spoilageRate",        "double",       "Daily percentage spoilage before storage adjustment"],
            ["marketPricePerCrate", "double",       "Price per crate (40 crates = 1 tonne)"],
        ],
        [
            ["Tomato(double prod, double pricePerCrate, int shelfLife, StorageType st, double spoilage)",
             "constructor", "Sets all fields; price = pricePerCrate * 40"],
            ["estimateTotalValue()", "double", "Returns productionVolume * price"],
            ["getPricePerTon()",     "double", "Returns the inherited price field (ZMW per tonne)"],
            ["getCommodityName()",   "String", "Returns \"Tomato\""],
        ]
    )

    # 5.5 StorageType
    class_section(doc, "5.5", "StorageType.java", "agritwin.model",
        "Two-constant enum that replaces fragile String comparisons for storage type. "
        "COLD represents a refrigerated or cold-store facility; OPEN represents ambient "
        "shed or open-air storage. Using an enum means the compiler catches typos at "
        "compile time rather than at runtime.",
        "Enum (type-safe constant set); Encapsulation (eliminates raw String storage-type checks).",
        [
            ["COLD", "StorageType", "Refrigerated storage — StorageLossSimulator applies 0.6x spoilage factor"],
            ["OPEN", "StorageType", "Ambient storage — full spoilage rate applied without reduction"],
        ],
        []
    )

    # 5.6 SimulationRecord
    class_section(doc, "5.6", "SimulationRecord.java", "agritwin.model",
        "Immutable per-period data snapshot capturing all nine simulation outputs for one "
        "month. Acts as the Data Transfer Object (DTO) between the service layer "
        "(SimulationRunner) and the presentation layer (ReportGenerator, FileHandler, "
        "DashboardController). Implements pipe-delimited serialisation for flat-file persistence.",
        "Encapsulation (all fields final, no setters); Composition (held in List<SimulationRecord> "
        "by SimulationRunner and ReportGenerator).",
        [
            ["month",                "int",    "Simulation month index (1-based)"],
            ["commodity",            "String", "Commodity name (\"Maize\" or \"Tomato\")"],
            ["production",           "double", "Tonnes produced this period"],
            ["demand",               "double", "Tonnes demanded this period"],
            ["surplusOrShortage",    "double", "Positive = surplus, negative = shortage (tonnes)"],
            ["waste",                "double", "Tonnes lost to storage decay or spoilage"],
            ["revenue",              "double", "Estimated revenue in ZMW"],
            ["foodSecurityIndicator","double", "Percentage of demand met (capped at 100%)"],
            ["policyAlerts",         "String", "Semicolon-joined policy alert messages for this period"],
        ],
        [
            ["toCsvLine()", "String",
             "Returns pipe-delimited line: month|commodity|...|policyAlerts (9 fields)"],
            ["fromCsvLine(String line)", "SimulationRecord (static)",
             "Splits on \"\\|\" with -1 limit to preserve empty trailing fields; parses all 9 fields; "
             "throws IllegalArgumentException if fewer than 9 tokens"],
        ]
    )

    # 5.7 SupplyDemandEngine
    class_section(doc, "5.7", "SupplyDemandEngine.java", "agritwin.service",
        "Core simulation engine. Provides stateless static methods that implement all "
        "supply-demand calculations: surplus/shortage, storage waste, revenue, price "
        "adjustment, food security indicator, seasonal demand modelling, and break-even "
        "price. A 0.1 price-elasticity coefficient is applied when supply and demand diverge.",
        "Encapsulation (all constants private static final); Single Responsibility Principle "
        "(dedicated engine with no side effects — pure functions).",
        [
            ["PRICE_ELASTICITY", "double (0.1, private static final)",
             "Fraction by which price changes per unit of relative surplus or shortage"],
            ["COLD_STORAGE_SPOILAGE_FACTOR", "double (0.6, private static final)",
             "Multiplier applied to waste when StorageType is COLD (40% reduction in waste)"],
        ],
        [
            ["calculateSurplus(double production, double demand)", "double",
             "Returns production minus demand (positive = surplus, negative = shortage)"],
            ["calculateWaste(double surplus, StorageType st)", "double",
             "Waste = surplus * 0.031 for OPEN, or * 0.031 * 0.6 for COLD"],
            ["calculateRevenue(double production, double waste, double price)", "double",
             "Returns (production - waste) * price"],
            ["adjustPrice(double price, double surplus, double demand)", "double",
             "Applies elasticity: newPrice = price * (1 - ELASTICITY * surplus/demand). "
             "Surplus is clipped to [-demand, demand] to prevent negative prices"],
            ["adjustPrice(double price, double surplusRatio)", "double",
             "Two-argument overload: newPrice = price * (1 - ELASTICITY * surplusRatio)"],
            ["calculateFoodSecurityIndicator(double prod, double demand)", "double",
             "Returns min(100.0, prod/demand * 100). Returns 0 if demand <= 0"],
            ["simulateSeasonalDemand(double baseDemand, int month)", "double",
             "Sinusoidal adjustment: baseDemand * (1 + 0.15 * sin(2*PI*(month-1)/12 - PI/2)). "
             "Peaks in July (month 7), troughs in January"],
            ["calculateBreakEvenPrice(double totalCost, double saleable)", "double",
             "Returns totalCost / saleable; returns 0 if saleable <= 0"],
        ]
    )

    # 5.8 StorageLossSimulator
    class_section(doc, "5.8", "StorageLossSimulator.java", "agritwin.service",
        "Simulates post-harvest loss for maize (multi-month compound decay) and tomatoes "
        "(daily spoilage). For maize, each moisture point above 14% adds 0.5 percentage "
        "points to the monthly loss rate. The simulator also computes a full month-by-month "
        "loss schedule and estimates the day at which tomato stock will reach critical "
        "spoilage (50% lost).",
        "Encapsulation (constants private static final, COLD_STORAGE_SPOILAGE_FACTOR public "
        "for PolicyEngine); Single Responsibility Principle.",
        [
            ["COLD_STORAGE_SPOILAGE_FACTOR", "double (0.6, public static final)",
             "Shared with SupplyDemandEngine — 40% waste reduction for cold storage"],
            ["MOISTURE_LOSS_THRESHOLD",       "double (14.0, private static final)",
             "Moisture percentage above which aflatoxin risk and extra loss rate apply"],
            ["MAX_MAIZE_MONTHLY_LOSS_RATE",   "double (20.0, private static final)",
             "Hard cap on monthly loss rate (%) regardless of moisture level"],
        ],
        [
            ["effectiveMaizeLossRate(Maize m)", "double",
             "base loss + 0.5 * max(0, moisture - 14); capped at MAX_MAIZE_MONTHLY_LOSS_RATE"],
            ["simulateMaizeLoss(Maize m, int months)", "double",
             "Compound decay: initial * (1 - rate/100)^months; returns remaining volume"],
            ["getMaizeLossSchedule(Maize m, int months)", "List<Double>",
             "Returns list of size months+1 where index 0 = initial volume and each "
             "subsequent element is volume after applying effectiveMaizeLossRate() once"],
            ["simulateTomatoSpoilage(Tomato t, int days)", "double",
             "Daily compound spoilage; applies COLD_STORAGE_SPOILAGE_FACTOR if COLD storage"],
            ["daysToCriticalSpoilage(Tomato t)", "int",
             "Returns ceil(50.0 / effectiveRate) where effectiveRate accounts for storage type"],
        ]
    )

    # 5.9 PolicyEngine
    class_section(doc, "5.9", "PolicyEngine.java", "agritwin.service",
        "Evaluates six Zambian government agricultural policy rules and returns structured "
        "Alert objects. Each Alert carries a message string and a Severity level (INFO, "
        "WARNING, or CRITICAL). The main public API is evaluateAllWithSeverity(), which "
        "returns a List<Alert>; evaluateAll() returns a semicolon-joined String for "
        "backward compatibility.",
        "Encapsulation; Single Responsibility; Inner Static Class (Alert); Composition "
        "(uses Severity enum).",
        [
            ["FRA_RESERVE_THRESHOLD",   "double (500000, public static final)",
             "National strategic reserve threshold in tonnes; stock below this triggers a CRITICAL alert"],
            ["EXPORT_SURPLUS_FRACTION", "double (0.20, public static final)",
             "If surplus > 20% of production, an export review WARNING is issued"],
            ["MAIZE_MAX_STORAGE_MONTHS","int (6, public static final)",
             "Maize stored beyond 6 months triggers a WARNING about quality deterioration"],
        ],
        [
            ["checkFraReserve(double stock)", "Alert",
             "CRITICAL if stock < FRA_RESERVE_THRESHOLD; INFO if adequate"],
            ["checkExportSurplus(double surplus, double production)", "Alert",
             "WARNING if surplus/production > EXPORT_SURPLUS_FRACTION; INFO otherwise"],
            ["checkMaizeStorageAge(int months)", "Alert",
             "WARNING if months >= MAIZE_MAX_STORAGE_MONTHS"],
            ["checkTomatoShelfLife(Tomato t, int daysStored)", "Alert",
             "CRITICAL if daysStored >= shelfLifeDays; WARNING if > 80% of shelf life elapsed"],
            ["checkAflatoxinRisk(Maize m, int consecutiveHighMoistureMonths)", "Alert",
             "CRITICAL if moisture > 14% AND consecutive months >= 3"],
            ["checkFarmerPriceFloor(double currentPrice, double floorPrice)", "Alert",
             "WARNING if currentPrice < floorPrice"],
            ["evaluateAllWithSeverity(double stock, double surplus, double prod, "
             "int maizeMonths, Maize m, int highMoistMonths, double price, double floor, "
             "Tomato t, int daysStored)", "List<Alert>",
             "Calls all six check methods; returns list of Alert objects with Severity"],
            ["evaluateAll(...)", "String",
             "Calls evaluateAllWithSeverity(); joins messages with \"; \""],
        ]
    )

    # 5.10 Severity
    class_section(doc, "5.10", "Severity.java", "agritwin.service",
        "Three-constant enum classifying the urgency of a PolicyEngine alert. Used by the "
        "PolicyEngine.Alert inner class and displayed in the GUI with colour coding (INFO "
        "= cyan, WARNING = yellow, CRITICAL = red).",
        "Enum (type-safe severity classification); Open/Closed Principle (new severity "
        "levels can be added without modifying callers).",
        [
            ["INFO",     "Severity", "Routine notice — no immediate action required"],
            ["WARNING",  "Severity", "Attention needed — monitor and plan corrective action"],
            ["CRITICAL", "Severity", "Immediate action required — risk to food security or crop quality"],
        ],
        []
    )

    # 5.11 ReportGenerator
    class_section(doc, "5.11", "ReportGenerator.java", "agritwin.service",
        "Formats simulation results into human-readable console reports, .txt exports, and "
        "RFC 4180-compliant CSV files. Depends on the RecordStore interface (Dependency "
        "Inversion Principle) rather than FileHandler directly, so the storage backend is "
        "swappable. The season summary aggregates alert frequencies using a HashMap.",
        "Dependency Inversion Principle (constructor takes RecordStore interface); "
        "Composition (holds a RecordStore reference); Single Responsibility.",
        [
            ["store", "RecordStore (private final)",
             "Injected persistence backend; used by exportStructured()"],
        ],
        [
            ["ReportGenerator(RecordStore store)", "constructor",
             "Injects the RecordStore dependency"],
            ["generateMonthlyReport(List<SimulationRecord>)", "String",
             "Builds a formatted text table of all records for console display"],
            ["generateSeasonSummary(List<SimulationRecord>)", "String",
             "Aggregates totals (production, waste, revenue), average FSI, alert counts by "
             "severity, and top-3 most-frequent alert messages using HashMap<String,Integer>"],
            ["exportToFile(List<SimulationRecord>, String filename)", "void",
             "Writes pipe-delimited .txt with two comment header lines using PrintWriter"],
            ["exportToCsv(List<SimulationRecord>, String filename)", "void",
             "Writes RFC 4180 CSV; policyAlerts field is double-quoted and internal quotes escaped"],
            ["exportStructured(List<SimulationRecord>, String filename)", "void",
             "Delegates to store.saveSimulation() for interface-defined persistence"],
        ]
    )

    # 5.12 SimulationRunner
    class_section(doc, "5.12", "SimulationRunner.java", "agritwin.service",
        "Orchestrates the full simulation state machine. Holds instances of "
        "SupplyDemandEngine, StorageLossSimulator, and PolicyEngine; runs the month-by-month "
        "maize loop or the single-period tomato simulation; accumulates SimulationRecord "
        "objects; and exposes the results list for downstream consumers.",
        "Composition (holds three service objects and a List<SimulationRecord>); "
        "Single Responsibility (coordinates, does not calculate).",
        [
            ["engine",      "SupplyDemandEngine",   "Supply-demand calculations"],
            ["lossSimulator","StorageLossSimulator", "Storage loss and spoilage calculations"],
            ["policyEngine", "PolicyEngine",         "Policy rule evaluation"],
            ["records",      "List<SimulationRecord>","Accumulates per-period snapshots"],
        ],
        [
            ["runMaizeSimulation(Maize m, int months, double annualDemand)", "List<SimulationRecord>",
             "Month loop: apply loss → calculate surplus → adjust price → evaluate policy → "
             "create SimulationRecord; returns accumulated records list"],
            ["runTomatoSimulation(Tomato t, int days)", "List<SimulationRecord>",
             "Single-period tomato run: spoilage → surplus → policy → record; returns records"],
            ["getRecords()", "List<SimulationRecord>",
             "Returns the accumulated records list (unmodifiable view)"],
        ]
    )

    # 5.13 RecordStore
    class_section(doc, "5.13", "RecordStore.java", "agritwin.util",
        "Persistence interface defining the contract for saving and loading simulation "
        "records. Provides the Dependency Inversion seam: all service-layer code that "
        "needs persistence declares RecordStore rather than FileHandler, so unit tests "
        "or future implementations can inject alternative backends (database, cloud, etc.).",
        "Interface (abstraction over persistence); Dependency Inversion Principle; "
        "Open/Closed Principle (new storage backends require no change to calling code).",
        [],
        [
            ["saveSimulation(List<SimulationRecord>, String filename) throws IOException", "void",
             "Persist a list of records to the named destination"],
            ["loadSimulation(String filename) throws IOException", "List<SimulationRecord>",
             "Read and deserialise records from the named source"],
        ]
    )

    # 5.14 FileHandler
    class_section(doc, "5.14", "FileHandler.java", "agritwin.util",
        "Concrete RecordStore implementation that saves and loads pipe-delimited .txt "
        "files in the reports/ directory. Two header comment lines are written at the top "
        "of each file (prefixed with #). During load, comment and blank lines are skipped; "
        "malformed lines are logged via java.util.logging.Logger rather than throwing, so "
        "a single bad line does not abort the entire load.",
        "Interface Implementation (implements RecordStore); Encapsulation; "
        "Composition (uses Logger); Exception Handling.",
        [
            ["HEADER", "String (private static final)",
             "Two-line comment block written at the top of every saved file"],
            ["logger", "Logger (private static final)",
             "java.util.logging.Logger for warning on malformed lines"],
        ],
        [
            ["saveSimulation(List<SimulationRecord>, String filename)", "void",
             "Creates reports/ if absent; writes HEADER then calls toCsvLine() for each record "
             "using PrintWriter with UTF-8 encoding"],
            ["loadSimulation(String filename)", "List<SimulationRecord>",
             "Reads file line by line; skips lines starting with # or blank; calls "
             "SimulationRecord.fromCsvLine(); logs WARNING and skips malformed lines"],
        ]
    )

    # 5.15 ConsoleUI
    class_section(doc, "5.15", "ConsoleUI.java", "agritwin.ui",
        "Text-based user interface driven by a Scanner. Presents a seven-option numbered "
        "menu in a loop; each option calls private helper methods to read parameters, "
        "delegate to SimulationRunner or ReportGenerator, and print results. The RecordStore "
        "field is declared as the interface type, demonstrating Dependency Inversion even "
        "in the UI layer.",
        "Composition (holds SimulationRunner, ReportGenerator, RecordStore); "
        "Dependency Inversion (RecordStore field typed as interface); "
        "Encapsulation; Single Responsibility (each menu option is a private method).",
        [
            ["scanner",   "Scanner (private final)",  "Wraps System.in in a try-with-resources block"],
            ["runner",    "SimulationRunner",          "Runs simulations on user demand"],
            ["generator", "ReportGenerator",           "Formats and exports reports"],
            ["store",     "RecordStore",               "Typed as interface; assigned a FileHandler instance"],
            ["records",   "List<SimulationRecord>",    "Holds current session records"],
        ],
        [
            ["start()", "void",
             "Prints banner; loops on the 7-option menu switch until option 5 (Exit) is chosen"],
            ["runMaize()", "void (private)",
             "Prompts for Maize parameters with bracketed defaults; calls runner.runMaizeSimulation()"],
            ["runTomato()", "void (private)",
             "Prompts for Tomato parameters; calls runner.runTomatoSimulation()"],
            ["viewReports()", "void (private)",
             "Calls generator.generateMonthlyReport() and generateSeasonSummary(); prints to stdout"],
            ["saveSimulation()", "void (private)",
             "Prompts for filename; calls generator.exportToFile()"],
            ["loadSimulation()", "void (private)",
             "Prompts for filename; calls store.loadSimulation(); updates records field"],
            ["exportCsv()", "void (private)",
             "Prompts for filename; calls generator.exportToCsv()"],
        ]
    )

    # 5.16 GuiApp
    class_section(doc, "5.16", "GuiApp.java", "agritwin.ui",
        "JavaFX Application bootstrap. Extends javafx.application.Application; its "
        "start() method delegates scene construction to DashboardController.buildScene(). "
        "The static launchApp(String[]) method is invoked reflectively by Main.launchGui() "
        "so that no JavaFX import is needed in Main.",
        "Inheritance (extends Application); Bridge pattern (static launchApp bridges "
        "reflection call to JavaFX launch mechanism); Composition (delegates to "
        "DashboardController).",
        [],
        [
            ["launchApp(String[] args)", "void (public static)",
             "Called reflectively by Main; invokes Application.launch(GuiApp.class, args)"],
            ["start(Stage stage)", "void (override)",
             "Delegates to DashboardController.buildScene(stage); sets title and shows stage"],
        ]
    )

    # 5.17 DashboardController
    class_section(doc, "5.17", "DashboardController.java", "agritwin.ui",
        "The largest class (~1500 lines). Builds the entire six-page JavaFX scene: sidebar, "
        "top bar, content area, and all six pages (Dashboard, Simulation, Storage, Policy "
        "Alerts, Reports, Information). Writes the dark CSS to a temp file at startup. "
        "Page navigation uses FadeTransition (200ms). After runSimulation() parses input "
        "fields and runs the SimulationRunner, it populates all pages.",
        "Composition (holds SimulationPage, ReportsPage, ReportDetailPage, all service "
        "classes); Observer pattern (page-select callbacks); MVC (controller updates views "
        "from model results); JavaFX Scene Graph.",
        [
            ["PG_DASHBOARD..PG_SETTINGS", "int constants (0-5)",
             "Index constants for the pages[] VBox array to avoid magic numbers"],
            ["pages", "VBox[] (private)",
             "Array of six page VBoxes; selectPage() swaps the visible one"],
            ["simRunner",   "SimulationRunner",    "Runs simulations when user clicks Run"],
            ["simPage",     "SimulationPage",      "Manages save-report lifecycle"],
            ["reportsPage", "ReportsPage",         "Manages report card grid"],
        ],
        [
            ["buildScene(Stage stage)", "void (public static)",
             "Entry point; creates root BorderPane; builds sidebar, topBar, and all 6 pages; "
             "writes CSS to temp file; shows 1280x720 window"],
            ["selectPage(int index)", "void (private)",
             "Applies FadeTransition (200ms, opacity 0→1) to swapped page; updates sidebar selection"],
            ["runSimulation()", "void (private)",
             "Parses all input fields; creates Maize/Tomato; calls simRunner; populates "
             "stat cards, charts, alerts, and StoragePage"],
            ["buildDashboardPage() .. buildInfoPage()", "VBox (private)",
             "One builder method per page; constructs layout nodes and wires event handlers"],
        ],
        "NAVY (#0F1535), TEAL (#00B5D8), and DARK (#1A1A2E) colour constants are defined "
        "as private static final String fields at the top of the class."
    )

    # 5.18 SimulationPage
    class_section(doc, "5.18", "SimulationPage.java", "agritwin.ui",
        "Manages the Save Report button lifecycle on the Simulation page. When the user "
        "clicks Save, a TextInputDialog prompts for a filename. performSave() creates the "
        "reports/ directory if absent, writes the file via ReportGenerator, and calls "
        "showSavedBadge() which adds a ReportSummary to an ObservableList (triggering the "
        "ReportsPage grid refresh). Hover animations use ScaleTransition.",
        "Composition (holds ReportGenerator, ObservableList<ReportSummary>); "
        "Observer pattern (ObservableList drives ReportsPage refresh); "
        "Event-driven programming (JavaFX button handlers).",
        [],
        [
            ["handleSaveClicked(List<SimulationRecord>)", "void",
             "Opens TextInputDialog; calls performSave() if user confirms"],
            ["performSave(List<SimulationRecord>, String name)", "void (private)",
             "Creates reports/ dir; calls generator.exportToFile(); calls showSavedBadge()"],
            ["showSavedBadge(String filename)", "void (private)",
             "Derives commodity/month-count from filename; adds ReportSummary to ObservableList; "
             "shows temporary green confirmation label"],
        ]
    )

    # 5.19 ReportsPage
    class_section(doc, "5.19", "ReportsPage.java", "agritwin.ui",
        "Displays a FlowPane grid of ReportCard tiles. A ListChangeListener on the "
        "shared ObservableList auto-refreshes the grid whenever SimulationPage adds a new "
        "report. navigateToDetail() replaces the page VBox's child with a ReportDetailPage "
        "view. deleteReport() removes a card and deletes the file from disk. "
        "refreshFromDisk() rescans the reports/ folder on startup.",
        "Observer pattern (ListChangeListener); Composition (holds ReportDetailPage reference); "
        "Encapsulation; Event-driven programming.",
        [],
        [
            ["buildView(ObservableList<ReportSummary>)", "VBox (public static)",
             "Creates FlowPane; attaches ListChangeListener; calls refreshFromDisk()"],
            ["navigateToDetail(ReportSummary)", "void (private)",
             "Loads full records via FileHandler.loadSimulation(); builds ReportDetailPage; "
             "swaps child of parent VBox"],
            ["deleteReport(ReportSummary, ReportCard)", "void (private)",
             "Shows confirmation Alert; uses ifPresentOrElse(); deletes file and removes card"],
            ["refreshFromDisk()", "void (private)",
             "Rescans reports/ directory; calls buildSummary() for each .txt file found"],
            ["buildSummary(File)", "ReportSummary (static)",
             "Derives commodity and period count from filename and first line of file"],
        ]
    )

    # 5.20 ReportDetailPage
    class_section(doc, "5.20", "ReportDetailPage.java", "agritwin.ui",
        "Builds the full report detail view for a single saved report: a policy card, "
        "a 3-series LineChart (production/demand/surplus), a BarChart (revenue), an "
        "AreaChart (FSI with 80%/60% reference lines), and a TableView with 9 columns. "
        "Charts animate in on load with staggered FadeTransitions (0ms and 300ms delay). "
        "The policy card uses a LinkedHashSet to deduplicate alert messages.",
        "Composition (multiple chart and table nodes); Builder pattern (buildView() "
        "orchestrates all sub-builders); JavaFX Scene Graph; Animation.",
        [],
        [
            ["buildView(List<SimulationRecord>)", "VBox (public static)",
             "Top-level builder; calls all sub-builders; calls animateCharts()"],
            ["buildFraPolicyCard(List<SimulationRecord>)", "VBox (private static)",
             "Extracts unique alert messages via LinkedHashSet; colour-codes by severity keyword"],
            ["buildSurplusChartCard(List<SimulationRecord>)", "VBox (private static)",
             "3-series LineChart: Production, Demand, Surplus per month"],
            ["buildRevenueChartCard(List<SimulationRecord>)", "VBox (private static)",
             "BarChart: one bar per month with revenue figure; tooltips via setOnMouseEntered"],
            ["buildFsiChartCard(List<SimulationRecord>)", "VBox (private static)",
             "AreaChart with FSI series plus two horizontal threshold lines at 80% and 60%"],
            ["buildDataTableCard(List<SimulationRecord>)", "VBox (private static)",
             "TableView with 9 TableColumns; each column uses PropertyValueFactory pattern"],
            ["animateCharts(List<Node>)", "void (private static)",
             "Staggered FadeTransitions: first chart at 0ms delay, second at 300ms delay"],
        ]
    )

    # 5.21 StatCard
    class_section(doc, "5.21", "StatCard.java", "agritwin.ui.components",
        "Reusable JavaFX StackPane that displays a metric label, animated numeric value, "
        "unit string, and a thin progress bar. The count-up animation runs 30 frames over "
        "600ms via a Timeline; the progress bar animates over 800ms. A static helper "
        "addHoverLift() attaches a TranslateTransition that lifts the card 3px on hover.",
        "Inheritance (extends StackPane); Encapsulation (DoubleProperty for progress); "
        "Animation (Timeline, KeyFrame, KeyValue); Composition.",
        [
            ["progressFraction", "DoubleProperty (private)",
             "Bound to the width of the progress bar fill rectangle; animated by animateProgress()"],
        ],
        [
            ["StatCard(String label, String value, String unit, double progress)",
             "constructor", "Builds layout: label VBox + progress bar row; sets dark styling"],
            ["animateCountUp(String targetText)", "void",
             "Parses numeric suffix from targetText; 30-frame Timeline interpolates 0 to target over 600ms"],
            ["animateProgress(double fraction)", "void",
             "800ms Timeline animates progressFraction property from 0 to fraction"],
            ["addHoverLift(Node node)", "void (public static)",
             "TranslateTransition Y=0 on exit, Y=-3 on hover enter; reusable across UI components"],
        ]
    )

    # 5.22 AlertPanel
    class_section(doc, "5.22", "AlertPanel.java", "agritwin.ui.components",
        "JavaFX VBox that displays the current FRA policy status. A gold border (#ECC94B) "
        "signals attention. The panel exposes setBodyText() for the controller to push "
        "updated alert content, and an onViewReport Runnable callback that is invoked when "
        "the user clicks 'View Full Report'.",
        "Inheritance (extends VBox); Callback pattern (Runnable onViewReport); "
        "Encapsulation; Event-driven programming.",
        [],
        [
            ["AlertPanel(String title)", "constructor",
             "Creates gold-bordered VBox with title label; adds 'View Full Report' button"],
            ["setBodyText(String text)", "void",
             "Public API for the controller to update displayed alert messages"],
            ["setOnViewReport(Runnable r)", "void",
             "Stores callback invoked on button click; decouples panel from controller logic"],
        ]
    )

    # 5.23 SidebarMenu
    class_section(doc, "5.23", "SidebarMenu.java", "agritwin.ui.components",
        "Dark navy sidebar navigation VBox. Six rows, each a StackPane containing an icon "
        "label, text label, and a transparent hover-overlay Rectangle. FadeTransition "
        "fades the overlay in on hover. setSelected() updates the active accent bar "
        "(a 3px cyan left border Rectangle) and makes the selected label bold.",
        "Inheritance (extends VBox); Composition (StackPane rows, arrays of labels); "
        "Animation (FadeTransition hover effect); Observer/Callback (IntConsumer onSelect).",
        [
            ["LABELS", "String[] (private static final)",
             "Six navigation labels: Dashboard, Simulation, Storage, Policy Alerts, Reports, Information"],
            ["ICONS",  "String[] (private static final)",
             "Unicode icon characters corresponding to each label"],
            ["rows",   "StackPane[] (private)",
             "Array of six row StackPanes; setSelected() iterates this array"],
        ],
        [
            ["SidebarMenu(IntConsumer onSelect)", "constructor",
             "Builds all 6 rows; wires click handlers that call onSelect.accept(index)"],
            ["setSelected(int index)", "void",
             "Shows cyan accent bar on selected row; bolds selected label; resets all others"],
        ]
    )

    # 5.24 ReportCard
    class_section(doc, "5.24", "ReportCard.java", "agritwin.ui.components",
        "Clickable VBox thumbnail card for the Reports page grid. Four builder methods "
        "construct the top row (filename + commodity badge), stats row (months, records), "
        "FRA status row, and a coloured progress strip. A Timeline hover effect raises the "
        "card on the Y axis. A context menu offers 'Open' and 'Copy Path'; the delete "
        "option shows a confirmation Alert before removing the file.",
        "Inheritance (extends VBox); Composition (inner rows, context menu); "
        "Animation (Timeline KeyValue on translateYProperty); Event-driven programming.",
        [],
        [
            ["ReportCard(ReportSummary, Runnable onOpen, Runnable onDelete)", "constructor",
             "Calls all four build methods; sets up hover and context menu"],
            ["buildTopRow(ReportSummary)", "HBox (private)",
             "Filename label + coloured commodity badge (green for Maize, orange for Tomato)"],
            ["buildStatsRow(ReportSummary)", "HBox (private)",
             "Month count and record count labels with icon prefixes"],
            ["buildFraRow(ReportSummary)",  "HBox (private)",
             "FRA status dot (red/green) derived from summary.isAboveFraThreshold()"],
            ["buildProgressRow(ReportSummary)", "HBox (private)",
             "Thin coloured strip representing average FSI percentage"],
            ["setupHoverEffect()", "void (private)",
             "Timeline: translateY → -4 on enter (150ms), → 0 on exit (150ms)"],
            ["setupContextMenu()", "void (private)",
             "ContextMenu with Open, Copy Path, and Delete items; Delete shows confirmation Alert"],
        ]
    )

    # 5.25 ReportSummary
    class_section(doc, "5.25", "ReportSummary.java", "agritwin.ui.model",
        "Immutable Data Transfer Object carrying the pre-computed summary fields needed "
        "to display a ReportCard in the Reports grid. All eight fields are final and set "
        "in the all-args constructor; no setters exist. This enforces the invariant that "
        "a ReportSummary is always fully populated.",
        "Encapsulation (all fields private final); Immutability (no setters); "
        "Value Object pattern.",
        [
            ["filename",           "String",  "File path to the .txt save file"],
            ["commodity",          "String",  "Commodity name derived from filename"],
            ["monthCount",         "int",     "Number of simulation months in the file"],
            ["recordCount",        "int",     "Total SimulationRecord lines in the file"],
            ["averageFsi",         "double",  "Mean food security indicator across all records"],
            ["totalRevenue",       "double",  "Sum of revenue across all records"],
            ["aboveFraThreshold",  "boolean", "True if average surplus > FRA_RESERVE_THRESHOLD"],
            ["dateCreated",        "String",  "ISO date string extracted from filename"],
        ],
        [
            ["ReportSummary(String filename, String commodity, int monthCount, "
             "int recordCount, double avgFsi, double totalRevenue, boolean fraOk, String date)",
             "constructor", "Sets all 8 fields; only constructor (no setters)"],
        ]
    )

    # 5.26 TestRunner
    class_section(doc, "5.26", "TestRunner.java", "agritwin",
        "Plain-main test suite with 18 test methods — no JUnit or testing framework "
        "required. Each test method follows the pattern: set up inputs, compute actual, "
        "compare to expected within EPS=0.5, print [PASS] or [FAIL], increment a fail "
        "counter. If any test fails, the JVM exits with code 1. Tests cover the full "
        "service layer: surplus, pricing, loss, spoilage, FSI, FRA alerts, file round-trip, "
        "exception validation, seasonal demand, moisture-adjusted loss, aflatoxin, season "
        "summary, and CSV export.",
        "Static methods (each test is a private static void method); "
        "Encapsulation (no shared mutable state between tests); "
        "Exception Handling (tests that expect exceptions use try/catch).",
        [
            ["EPS", "double (0.5, private static final)",
             "Tolerance for floating-point comparisons in test assertions"],
            ["failCount", "int (private static)",
             "Incremented when any test assertion fails; checked at end of main()"],
        ],
        [
            ["main(String[] args)", "void",
             "Calls all 18 test methods in order; prints total pass/fail count; "
             "calls System.exit(1) if failCount > 0"],
            ["test1_surplus() .. test18_csvExportContainsHeader()", "void (private static)",
             "Each test exercises one behaviour; uses assert_() helper to print PASS/FAIL and "
             "increment failCount on mismatch"],
            ["assert_(String name, boolean cond)", "void (private static)",
             "Prints '[PASS] name' or '[FAIL] name'; increments failCount if !cond"],
        ],
        "test13_validationException() and test14_tomatoValidation() test that "
        "IllegalArgumentException is thrown with the correct message for bad constructor "
        "arguments — these are the only tests that use try/catch flow."
    )

# ─── SECTION 6 — DATA FLOW DIAGRAMS ───────────────────────────────────────────

def build_section6(doc):
    add_h1(doc, "Section 6 — Data Flow Diagrams")
    add_body(doc,
        "The following ASCII diagrams illustrate how data moves through AgriTwin-ZM "
        "during the three most important operations: running a maize simulation, saving "
        "and loading a report, and the class dependency structure."
    )

    add_h2(doc, "6.1  Maize Simulation Run — Data Flow")
    add_code_block(doc,
        "USER INPUT (ConsoleUI / DashboardController)\n"
        "  productionVolume, price, storageCapacity, moistureLevel, lossPercentagePerMonth,\n"
        "  months, annualDemand\n"
        "       |\n"
        "       v\n"
        "  new Maize(prod, price, cap, moist, loss)   <-- validated in constructor\n"
        "       |\n"
        "       v\n"
        "SimulationRunner.runMaizeSimulation(maize, months, annualDemand)\n"
        "  |\n"
        "  +-- for each month 1..N:\n"
        "  |     |\n"
        "  |     +--[1] StorageLossSimulator.effectiveMaizeLossRate(maize)\n"
        "  |     |         moistureLevel > 14? add 0.5 pp per degree over threshold\n"
        "  |     |         cap at MAX_MAIZE_MONTHLY_LOSS_RATE (20%)\n"
        "  |     |\n"
        "  |     +--[2] StorageLossSimulator.simulateMaizeLoss(maize, 1 month)\n"
        "  |     |         remaining = initial * (1 - rate/100)^1\n"
        "  |     |\n"
        "  |     +--[3] SupplyDemandEngine.simulateSeasonalDemand(annualDemand/12, month)\n"
        "  |     |         monthlyDemand = base * (1 + 0.15 * sin(2pi*(m-1)/12 - pi/2))\n"
        "  |     |\n"
        "  |     +--[4] SupplyDemandEngine.calculateSurplus(remaining, demand)\n"
        "  |     |\n"
        "  |     +--[5] SupplyDemandEngine.calculateWaste(surplus, OPEN)\n"
        "  |     |\n"
        "  |     +--[6] SupplyDemandEngine.calculateRevenue(remaining, waste, price)\n"
        "  |     |\n"
        "  |     +--[7] SupplyDemandEngine.adjustPrice(price, surplus, demand)\n"
        "  |     |         price *= (1 - 0.1 * surplus/demand)\n"
        "  |     |\n"
        "  |     +--[8] SupplyDemandEngine.calculateFoodSecurityIndicator(remaining, demand)\n"
        "  |     |         fsi = min(100, remaining/demand * 100)\n"
        "  |     |\n"
        "  |     +--[9] PolicyEngine.evaluateAll(stock, surplus, prod, months,\n"
        "  |     |                                maize, highMoistMonths, price, floor)\n"
        "  |     |         -> semicolon-joined alert string\n"
        "  |     |\n"
        "  |     +--[10] new SimulationRecord(month, \"Maize\", prod, demand, surplus,\n"
        "  |                                   waste, revenue, fsi, alerts)\n"
        "  |               added to records List\n"
        "  |\n"
        "  v\n"
        "List<SimulationRecord>  -->  ReportGenerator / FileHandler / DashboardController"
    )

    add_h2(doc, "6.2  Save and Load Report — Data Flow")
    add_code_block(doc,
        "SAVE:\n"
        "  User clicks 'Save Report' (GUI: SimulationPage) or enters option 4 (Console)\n"
        "       |\n"
        "       v\n"
        "  ReportGenerator.exportToFile(records, filename)\n"
        "       |\n"
        "       v\n"
        "  FileHandler.saveSimulation(records, filename)\n"
        "    1. Creates reports/ directory if absent\n"
        "    2. Writes HEADER comment lines\n"
        "    3. For each record: calls SimulationRecord.toCsvLine()\n"
        "       -> \"month|commodity|prod|demand|surplus|waste|rev|fsi|alerts\"\n"
        "    4. Closes PrintWriter (UTF-8)\n"
        "\n"
        "LOAD:\n"
        "  User selects a report (GUI: ReportsPage click) or enters option 5 (Console)\n"
        "       |\n"
        "       v\n"
        "  FileHandler.loadSimulation(filename)\n"
        "    1. Opens file with BufferedReader\n"
        "    2. Skips lines starting with '#' or blank lines\n"
        "    3. For each data line: SimulationRecord.fromCsvLine(line)\n"
        "       -> split on \"\\|\" with limit -1  -> parse 9 fields\n"
        "    4. Malformed lines -> Logger.WARNING (not thrown)\n"
        "    5. Returns List<SimulationRecord>\n"
        "       |\n"
        "       v\n"
        "  DashboardController (GUI) or ConsoleUI populates display from records"
    )

    add_h2(doc, "6.3  Class Dependency Map")
    add_code_block(doc,
        "                    Main\n"
        "                   /    \\\n"
        "            ConsoleUI   GuiApp\n"
        "                |           \\\n"
        "                |       DashboardController\n"
        "                |        /   |   |   \\\n"
        "                |  SimPage ReportsPage ReportDetailPage\n"
        "                |            |\n"
        "                |       ReportCard  StatCard  AlertPanel  SidebarMenu\n"
        "                |            |\n"
        "                |       ReportSummary\n"
        "                |\n"
        "         SimulationRunner\n"
        "          /       |       \\\n"
        "  SDE    SLS    PolicyEngine\n"
        "   |      |         |\n"
        "  Maize Tomato  Severity\n"
        "   \\     /\n"
        "  Commodity (abstract)\n"
        "         |\n"
        "   StorageType (enum)\n"
        "\n"
        "  ReportGenerator --> RecordStore (interface) <-- FileHandler\n"
        "  FileHandler --> SimulationRecord.fromCsvLine / toCsvLine\n"
        "\n"
        "  SDE = SupplyDemandEngine   SLS = StorageLossSimulator"
    )

# ─── SECTION 7 — ALGORITHMS AND FORMULAS ──────────────────────────────────────

def build_section7(doc):
    add_h1(doc, "Section 7 — Algorithms and Formulas")
    add_body(doc,
        "This section documents every mathematical formula used in AgriTwin-ZM, "
        "giving the variable names as they appear in the source code, the formula, "
        "and a worked example."
    )

    add_h2(doc, "7.1  Compound Monthly Maize Storage Loss")
    add_body(doc,
        "Source: StorageLossSimulator.simulateMaizeLoss() and effectiveMaizeLossRate(). "
        "Maize in store decays each month at an effective rate that depends on moisture."
    )
    add_code_block(doc,
        "effectiveRate = lossPercentagePerMonth\n"
        "              + 0.5 * max(0, moistureLevel - 14.0)\n"
        "effectiveRate = min(effectiveRate, 20.0)   // hard cap\n"
        "\n"
        "remaining(months) = initialVolume * (1 - effectiveRate / 100) ^ months\n"
        "\n"
        "Example: initial=200000t, lossRate=3%, moisture=16%\n"
        "  effectiveRate = 3 + 0.5*(16-14) = 3 + 1.0 = 4.0%\n"
        "  remaining after 1 month = 200000 * (1 - 0.04)^1 = 200000 * 0.96 = 192000t\n"
        "  remaining after 6 months = 200000 * 0.96^6 = 200000 * 0.7828 = 156560t"
    )

    add_h2(doc, "7.2  Price Elasticity Adjustment")
    add_body(doc,
        "Source: SupplyDemandEngine.adjustPrice(). When supply diverges from demand, "
        "market price moves proportionally. PRICE_ELASTICITY = 0.1."
    )
    add_code_block(doc,
        "surplusRatio = surplus / demand           // surplus = production - demand\n"
        "newPrice     = oldPrice * (1 - ELASTICITY * surplusRatio)\n"
        "\n"
        "If surplusRatio > 0 (oversupply):  price falls\n"
        "If surplusRatio < 0 (shortage):    price rises\n"
        "surplusRatio is clipped to [-1, 1] to prevent negative prices\n"
        "\n"
        "Example: price=250 ZMW/t, production=200000t, demand=13083t\n"
        "  surplus     = 200000 - 13083 = 186917t\n"
        "  surplusRatio = 186917 / 13083 = 14.28  -> clipped to 1.0\n"
        "  newPrice     = 250 * (1 - 0.1 * 1.0) = 250 * 0.9 = 225 ZMW/t"
    )

    add_h2(doc, "7.3  Seasonal Demand Modelling")
    add_body(doc,
        "Source: SupplyDemandEngine.simulateSeasonalDemand(). Monthly demand follows a "
        "sinusoidal curve that peaks in July (dry season, month 7) and troughs in January "
        "(harvest season, month 1)."
    )
    add_code_block(doc,
        "monthlyDemand = baseDemand * (1 + 0.15 * sin(2*PI*(month-1)/12 - PI/2))\n"
        "\n"
        "  month=1 (Jan): sin(-PI/2) = -1.0  -> demand = base * 0.85  (lowest)\n"
        "  month=7 (Jul): sin(PI/2)  = +1.0  -> demand = base * 1.15  (highest)\n"
        "\n"
        "Example: annualDemand=157000t, baseDemand = 157000/12 = 13083t/month\n"
        "  Jan demand = 13083 * 0.85 = 11121t\n"
        "  Jul demand = 13083 * 1.15 = 15045t"
    )

    add_h2(doc, "7.4  Food Security Indicator (FSI)")
    add_body(doc, "Source: SupplyDemandEngine.calculateFoodSecurityIndicator().")
    add_code_block(doc,
        "FSI = min(100.0, (production / demand) * 100)\n"
        "\n"
        "  FSI = 100%  -> all demand met (food secure)\n"
        "  FSI < 80%   -> WARNING threshold (GUI shows amber)\n"
        "  FSI < 60%   -> CRITICAL threshold (GUI shows red)\n"
        "\n"
        "Example: production=192000t, demand=13083t\n"
        "  FSI = min(100, 192000/13083 * 100) = min(100, 1467.5%) = 100.0%"
    )

    add_h2(doc, "7.5  Tomato Spoilage")
    add_body(doc,
        "Source: StorageLossSimulator.simulateTomatoSpoilage(). Daily compound spoilage "
        "is adjusted by the COLD_STORAGE_SPOILAGE_FACTOR (0.6) when StorageType is COLD."
    )
    add_code_block(doc,
        "effectiveRate = spoilageRate * (storageType==COLD ? 0.6 : 1.0)\n"
        "remaining(days) = initialVolume * (1 - effectiveRate/100) ^ days\n"
        "\n"
        "daysToCriticalSpoilage = ceil(50.0 / effectiveRate)\n"
        "  (50% of stock lost = 'critical' threshold)\n"
        "\n"
        "Example: 5000 crates, spoilage=2%/day, OPEN storage, 7 days\n"
        "  remaining = 5000 * (1-0.02)^7 = 5000 * 0.8681 = 4341 crates\n"
        "  daysToCritical = ceil(50/2) = 25 days"
    )

    add_h2(doc, "7.6  Aflatoxin Risk Detection")
    add_body(doc,
        "Source: PolicyEngine.checkAflatoxinRisk(). Aflatoxin is a fungal toxin that "
        "proliferates when maize moisture content exceeds 14% for extended periods."
    )
    add_code_block(doc,
        "if (maize.getMoistureLevel() > 14.0 AND consecutiveHighMoistureMonths >= 3)\n"
        "    -> CRITICAL alert: \"AFLATOXIN RISK: moisture > 14% for 3+ months\"\n"
        "\n"
        "PolicyEngine tracks consecutiveHighMoistureMonths externally (passed as argument)\n"
        "SimulationRunner increments this counter each month moisture remains above threshold"
    )

    add_h2(doc, "7.7  Revenue Calculation")
    add_body(doc, "Source: SupplyDemandEngine.calculateRevenue().")
    add_code_block(doc,
        "revenue = (production - waste) * pricePerTonne\n"
        "\n"
        "Example: production=192000t, waste=5952t, price=225 ZMW/t\n"
        "  revenue = (192000 - 5952) * 225 = 186048 * 225 = 41,860,800 ZMW"
    )

# ─── SECTION 8 — FILE FORMATS ─────────────────────────────────────────────────

def build_section8(doc):
    add_h1(doc, "Section 8 — File Formats")

    add_h2(doc, "8.1  Pipe-Delimited Simulation Save File (.txt)")
    add_body(doc,
        "Saved by FileHandler.saveSimulation() into the reports/ directory. Each file "
        "begins with two comment lines (starting with #) followed by one data line per "
        "SimulationRecord. Fields are delimited by | (pipe). The file is UTF-8 encoded."
    )
    add_code_block(doc,
        "# AgriTwin-ZM v1.0 simulation file (pipe-delimited)\n"
        "# month|commodity|production|demand|surplusOrShortage|waste|revenue"
        "|foodSecurityIndicator|policyAlerts\n"
        "1|Maize|213400.0|13083.333333333334|200316.66666666666|6600.0"
        "|5.128666666666667E7|100.0|RESERVE ALERT (stock 200317t below FRA "
        "threshold 500000t); EXPORT REVIEW REQUIRED (surplus 93.9% of production)"
        "; STORAGE OK\n"
        "2|Maize|194307.1...|13083.3...|181223.8...|6009.5|...|100.0|..."
    )
    make_table(doc,
        ["Field #", "Field Name", "Type", "Notes"],
        [
            ["1", "month",                "int",    "1-based month index"],
            ["2", "commodity",            "String", "\"Maize\" or \"Tomato\""],
            ["3", "production",           "double", "Tonnes produced this period"],
            ["4", "demand",               "double", "Tonnes demanded this period"],
            ["5", "surplusOrShortage",    "double", "Positive surplus, negative shortage"],
            ["6", "waste",                "double", "Tonnes lost to decay/spoilage"],
            ["7", "revenue",              "double", "ZMW revenue (scientific notation for large values)"],
            ["8", "foodSecurityIndicator","double", "Percentage demand met (0.0–100.0)"],
            ["9", "policyAlerts",         "String", "Semicolon-separated alert messages; may contain commas"],
        ],
        col_widths=[1.5, 4, 2, 8.5]
    )

    add_h2(doc, "8.2  CSV Export (.csv)")
    add_body(doc,
        "Written by ReportGenerator.exportToCsv() / FileHandler. RFC 4180 compliant. "
        "The policyAlerts field is always enclosed in double quotes; any embedded double "
        "quotes are escaped as \"\". The header row is always present."
    )
    add_code_block(doc,
        "month,commodity,production,demand,surplusOrShortage,waste,revenue,"
        "foodSecurityIndicator,policyAlerts\n"
        "1,Maize,213400.0,13083.33,200316.67,6600.0,51286666.67,100.0,"
        "\"RESERVE ALERT; EXPORT REVIEW REQUIRED; STORAGE OK\"\n"
        "2,Maize,194307.17,..."
    )

    add_h2(doc, "8.3  reports/ Directory Structure")
    add_body(doc,
        "The reports/ directory is created automatically the first time the user saves a "
        "report. File naming convention: [Commodity]_Sim_[YYYY-MM-DD].txt. If the same "
        "name is used twice in one session the file is overwritten."
    )
    add_code_block(doc,
        "reports/\n"
        "  Maize_Sim_2026-05-10.txt\n"
        "  Tomato_Sim_2026-05-10.txt\n"
        "  Maize_Sim_2026-05-11.txt\n"
        "  ...\n"
        "\n"
        "CSV exports can be placed anywhere the user specifies; there is no enforced "
        "directory for CSV output."
    )

# ─── SECTION 9 — TESTING REFERENCE ────────────────────────────────────────────

def build_section9(doc):
    add_h1(doc, "Section 9 — Testing Reference")
    add_body(doc,
        "AgriTwin-ZM uses a plain-main test suite (TestRunner.java, 18 tests, no JUnit). "
        "Run with: java -cp out agritwin.TestRunner. All 18 tests must print [PASS]. "
        "The floating-point tolerance (EPS) is 0.5."
    )
    add_h2(doc, "9.1  Test Case Table")
    make_table(doc,
        ["#", "Test Name", "What Is Tested", "Key Assertion"],
        [
            ["1",  "test1_surplus",
             "SupplyDemandEngine.calculateSurplus()",
             "surplus(200000, 13083) = 186917 (within EPS)"],
            ["2",  "test2_priceAdjustment",
             "SupplyDemandEngine.adjustPrice() — oversupply drives price down",
             "adjustPrice(250, 186917, 13083) < 250"],
            ["3",  "test3_maizeLoss",
             "StorageLossSimulator.simulateMaizeLoss() — 1-month compound decay",
             "remaining after 1 month with 3% loss rate within EPS of expected"],
            ["4",  "test4_foodSecurity",
             "SupplyDemandEngine.calculateFoodSecurityIndicator() — capped at 100%",
             "FSI(200000, 13083) == 100.0"],
            ["5",  "test5_fraAlert",
             "PolicyEngine.checkFraReserve() — stock below threshold triggers CRITICAL",
             "alert for stock=300000t contains 'RESERVE ALERT'"],
            ["6",  "test6_tomatoSpoilage",
             "StorageLossSimulator.simulateTomatoSpoilage() — OPEN storage 7 days",
             "remaining within EPS of 5000*(0.98)^7"],
            ["7",  "test7_revenue",
             "SupplyDemandEngine.calculateRevenue()",
             "revenue(200000, 6200, 250) == (200000-6200)*250 within EPS"],
            ["8",  "test8_fileRoundTrip",
             "FileHandler save → load round-trip preserves all 9 fields",
             "loaded record fields match original record within EPS"],
            ["9",  "test9_exportSurplusAlert",
             "PolicyEngine.checkExportSurplus() — surplus > 20% of production",
             "alert contains 'EXPORT REVIEW REQUIRED'"],
            ["10", "test10_seasonalDemand",
             "SupplyDemandEngine.simulateSeasonalDemand() — July peak",
             "demand(month=7) > demand(month=1)"],
            ["11", "test11_moistureLossAdjustment",
             "StorageLossSimulator.effectiveMaizeLossRate() — moisture 16% adds 1pp",
             "effectiveRate(3%, moisture=16%) == 4.0 within EPS"],
            ["12", "test12_aflatoxinAlert",
             "PolicyEngine.checkAflatoxinRisk() — 3 consecutive high-moisture months",
             "alert contains 'AFLATOXIN RISK'"],
            ["13", "test13_validationException",
             "Maize constructor — negative production throws IllegalArgumentException",
             "exception thrown with expected message"],
            ["14", "test14_tomatoValidation",
             "Tomato constructor — zero spoilage rate throws IllegalArgumentException",
             "exception thrown with expected message"],
            ["15", "test15_coldStorageReduction",
             "StorageLossSimulator.simulateTomatoSpoilage() COLD vs OPEN",
             "cold-storage remaining > open-storage remaining after same days"],
            ["16", "test16_seasonSummary",
             "ReportGenerator.generateSeasonSummary() aggregates totals correctly",
             "summary contains total production and average FSI within EPS"],
            ["17", "test17_breakEvenPrice",
             "SupplyDemandEngine.calculateBreakEvenPrice()",
             "breakEven(50000000, 200000) == 250.0 within EPS"],
            ["18", "test18_csvExportContainsHeader",
             "ReportGenerator.exportToCsv() — output file starts with CSV header row",
             "first line of CSV file equals expected header string"],
        ],
        col_widths=[0.7, 4.3, 5.5, 5.5]
    )
    add_body(doc,
        "To run tests: compile with build.bat then execute 'java -cp out agritwin.TestRunner'. "
        "Expected output: 18 lines reading '[PASS] testN_name' followed by "
        "'All tests passed.' and exit code 0.",
        italic=True
    )

# ─── SECTION 10 — FUTURE DEVELOPMENT NOTES ────────────────────────────────────

def build_section10(doc):
    add_h1(doc, "Section 10 — Future Development Notes")
    add_body(doc,
        "This section documents known extension points, hardcoded values that would "
        "need to be externalised for production use, and planned features already "
        "partially scaffolded in the codebase."
    )

    add_h2(doc, "10.1  Hardcoded Policy Constants")
    add_body(doc,
        "The following PolicyEngine constants are derived from current Zambian government "
        "policy and would need to be updated or made user-configurable for a different "
        "country or policy environment:"
    )
    make_table(doc,
        ["Constant", "Current Value", "Source / Note"],
        [
            ["FRA_RESERVE_THRESHOLD",    "500,000 t",
             "Zambian Food Reserve Agency strategic reserve threshold"],
            ["EXPORT_SURPLUS_FRACTION",  "0.20 (20%)",
             "Trigger level for export review recommendation"],
            ["MAIZE_MAX_STORAGE_MONTHS", "6 months",
             "Agronomic guideline for quality deterioration in ambient storage"],
            ["MOISTURE_LOSS_THRESHOLD",  "14.0%",
             "Moisture level above which fungal growth (aflatoxin) risk escalates"],
            ["PRICE_ELASTICITY",         "0.1",
             "Simplified elasticity coefficient; real markets may vary by 0.05–0.3"],
        ],
        col_widths=[5, 3, 8]
    )

    add_h2(doc, "10.2  PDF Export (Planned — PDFBox Bundled)")
    add_body(doc,
        "pdfbox-app-3.0.1.jar is already bundled in lib/ and included in the "
        "build_with_javafx.bat classpath. A PDF export feature would add a "
        "ReportGenerator.exportToPdf() method using PDDocument and PDPage from PDFBox. "
        "The method signature would match exportToFile() and exportToCsv() so "
        "existing callers require no change."
    )

    add_h2(doc, "10.3  Database Backend")
    add_body(doc,
        "Because all persistence is hidden behind the RecordStore interface, swapping "
        "FileHandler for a SQLite or PostgreSQL backend requires only a new class that "
        "implements RecordStore. ConsoleUI and DashboardController would need no changes — "
        "only the dependency injection site (the line 'RecordStore store = new FileHandler()') "
        "would need updating."
    )

    add_h2(doc, "10.4  Additional Commodities")
    add_body(doc,
        "To add a third commodity (e.g. groundnuts), extend Commodity, implement "
        "estimateTotalValue() and getCommodityName(), then add a corresponding simulation "
        "method to SimulationRunner. The PolicyEngine may need commodity-specific rules. "
        "No existing classes would require modification, satisfying the Open/Closed Principle."
    )

    add_h2(doc, "10.5  Network / Cloud Sync")
    add_body(doc,
        "A cloud-sync RecordStore implementation could call a REST API instead of writing "
        "a local file. The interface contract (saveSimulation, loadSimulation) already "
        "declares IOException, which covers network errors. A future CloudHandler class "
        "would need only the RecordStore interface — the rest of the application is "
        "already decoupled."
    )

    add_h2(doc, "10.6  JavaFX Chart Export to Image")
    add_body(doc,
        "DashboardController already creates the three ReportDetailPage charts as JavaFX "
        "nodes. Exporting a chart to PNG would use the JavaFX WritableImage and "
        "javax.imageio.ImageIO APIs. This is a one-method addition to ReportDetailPage "
        "and requires no architectural changes."
    )

# ─── SECTION 11 — GLOSSARY ────────────────────────────────────────────────────

def build_section11(doc):
    add_h1(doc, "Section 11 — Glossary")
    add_body(doc,
        "Domain and technical terms used throughout AgriTwin-ZM and this documentation."
    )
    make_table(doc,
        ["Term", "Definition"],
        [
            ["Aflatoxin",
             "A carcinogenic mycotoxin produced by Aspergillus fungi in maize stored at "
             "moisture > 14%. PolicyEngine raises a CRITICAL alert when moisture has "
             "exceeded the threshold for 3+ consecutive months."],
            ["Compound Decay",
             "A decay model where the loss in each period is a fixed percentage of the "
             "remaining volume (not the original volume). AgriTwin-ZM applies this monthly "
             "to maize: remaining = initial * (1 - rate)^months."],
            ["Digital Twin",
             "A software simulation of a real-world system that mirrors its behaviour "
             "and allows 'what-if' scenario exploration. AgriTwin-ZM is a digital twin "
             "of the Zambian maize and tomato supply chains."],
            ["Dependency Inversion Principle (DIP)",
             "An OOP design principle stating that high-level modules should depend on "
             "abstractions (interfaces), not concrete implementations. In AgriTwin-ZM, "
             "ReportGenerator and ConsoleUI both depend on RecordStore, not FileHandler."],
            ["DTO (Data Transfer Object)",
             "A simple object that carries data between layers with no business logic. "
             "SimulationRecord and ReportSummary are DTOs in this project."],
            ["Food Reserve Agency (FRA)",
             "The Zambian government agency responsible for maintaining national strategic "
             "grain reserves. The 500,000-tonne threshold in PolicyEngine models the "
             "FRA's minimum reserve requirement."],
            ["Food Security Indicator (FSI)",
             "The percentage of monthly demand that can be satisfied by available "
             "production. FSI = min(100, production/demand * 100)."],
            ["JavaFX",
             "An optional Java UI toolkit used for AgriTwin-ZM's graphical dashboard. "
             "Requires a separate SDK download; loaded reflectively so console mode "
             "works without it."],
            ["Pipe-delimited",
             "A text file format where fields are separated by the '|' character. "
             "Used in AgriTwin-ZM save files because commas appear in policy alert messages, "
             "making CSV less safe without quoting."],
            ["PolicyEngine.Alert",
             "An inner static class holding a message String and a Severity enum value "
             "(INFO / WARNING / CRITICAL). Returned by evaluateAllWithSeverity()."],
            ["Price Elasticity",
             "The responsiveness of price to changes in supply relative to demand. "
             "AgriTwin-ZM uses a simplified coefficient of 0.1: a 10% surplus reduces "
             "price by 1%."],
            ["RecordStore",
             "The persistence interface in agritwin.util that defines saveSimulation() "
             "and loadSimulation(). FileHandler is the only current implementation."],
            ["Seasonal Demand",
             "The sinusoidal adjustment applied to monthly maize demand to reflect "
             "Zambian consumption patterns — high in the dry season (July), low at harvest "
             "time (January)."],
            ["SimulationRecord",
             "An immutable snapshot of one simulation period: 9 fields covering production, "
             "demand, surplus, waste, revenue, FSI, and policy alerts for that month."],
            ["StorageLossSimulator",
             "The service class that models post-harvest loss for maize (compound decay) "
             "and tomatoes (daily spoilage). COLD_STORAGE_SPOILAGE_FACTOR = 0.6 reduces "
             "waste by 40% for cold-stored commodities."],
            ["StorageType",
             "An enum with two constants: COLD (refrigerated) and OPEN (ambient). "
             "Replaces String comparisons for type safety."],
            ["Surplus / Shortage",
             "Surplus = production − demand (positive). Shortage = negative surplus. "
             "Drives price adjustment and FRA export-review alerts."],
            ["ZMW",
             "Zambian Kwacha — the currency used for revenue and price figures throughout "
             "the simulation."],
        ],
        col_widths=[5, 11]
    )

# ─── MAIN ─────────────────────────────────────────────────────────────────────

def main():
    doc = Document()

    # Remove default styles that would conflict
    for style in doc.styles:
        if style.name in ('Normal',):
            style.font.name = "Arial"
            style.font.size = Pt(11)

    build_cover(doc)
    build_toc(doc)
    build_section1(doc)
    build_section2(doc)
    build_section3(doc)
    build_section4(doc)
    build_section5(doc)
    build_section6(doc)
    build_section7(doc)
    build_section8(doc)
    build_section9(doc)
    build_section10(doc)
    build_section11(doc)

    out_path = "AgriTwin_ZM_Documentation.docx"
    doc.save(out_path)
    print(f"Saved: {out_path}")

if __name__ == "__main__":
    main()
