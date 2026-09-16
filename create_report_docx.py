import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls

def set_cell_background(cell, fill_hex):
    tcPr = cell._element.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._element.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def create_docx():
    doc = docx.Document()
    
    # Page Margins (1 inch everywhere)
    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        
    # Styles setup
    style_normal = doc.styles['Normal']
    font = style_normal.font
    font.name = 'Calibri'
    font.size = Pt(11)
    font.color.rgb = RGBColor(0x22, 0x22, 0x22)
    
    # --- COVER PAGE ---
    p_inst = doc.add_paragraph()
    p_inst.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_inst = p_inst.add_run("KITALE NATIONAL POLYTECHNIC\n")
    run_inst.bold = True
    run_inst.font.size = Pt(18)
    run_inst.font.color.rgb = RGBColor(0x00, 0x33, 0x99) # KNP Royal Blue
    
    run_dept = p_inst.add_run("DEPARTMENT OF AGRICULTURE AND COMPUTING AND INFORMATICS\n")
    run_dept.bold = True
    run_dept.font.size = Pt(12)
    run_dept.font.color.rgb = RGBColor(0x42, 0x42, 0x42)
    
    p_space1 = doc.add_paragraph()
    p_space1.paragraph_format.space_after = Pt(24)
    
    p_proj = doc.add_paragraph()
    p_proj.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_subt = p_proj.add_run("PROJECT REPORT & INNOVATION PROPOSAL\n\n")
    run_subt.font.size = Pt(12)
    run_subt.font.color.rgb = RGBColor(0x75, 0x75, 0x75)
    
    run_title = p_proj.add_run("DIGITAL PIG FARM MANAGER (PHMS)\n")
    run_title.bold = True
    run_title.font.size = Pt(24)
    run_title.font.color.rgb = RGBColor(0x0D, 0x47, 0xA1) # Royal Blue
    
    run_desc = p_proj.add_run("An Intelligent, Offline-First Mobile Application for Precision Pig Husbandry, Health Tracking, Least-Cost Feed Formulation, Market Linkage, and Financial Analytics")
    run_desc.italic = True
    run_desc.font.size = Pt(11.5)
    run_desc.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
    
    p_space2 = doc.add_paragraph()
    p_space2.paragraph_format.space_after = Pt(36)
    
    # Cover details table
    table_cover = doc.add_table(rows=3, cols=2)
    table_cover.alignment = WD_TABLE_ALIGNMENT.CENTER
    table_cover.autofit = False
    
    details = [
        ("EVENT:", "KITALE AGRICULTURAL SOCIETY OF KENYA (ASK) SHOW 2026"),
        ("SHOW THEME:", "\"Promoting Climate-Smart Agriculture, Technological Innovation, and Trade Initiatives for Sustainable Economic Growth\""),
        ("SUBMISSION DATE:", "September 2026")
    ]
    
    for idx, (label, val) in enumerate(details):
        row = table_cover.rows[idx]
        cell_lbl, cell_val = row.cells[0], row.cells[1]
        cell_lbl.width = Inches(2.2)
        cell_val.width = Inches(4.3)
        
        p_lbl = cell_lbl.paragraphs[0]
        r_l = p_lbl.add_run(label)
        r_l.bold = True
        r_l.font.size = Pt(10.5)
        r_l.font.color.rgb = RGBColor(0x00, 0x33, 0x99)
        
        p_v = cell_val.paragraphs[0]
        r_v = p_v.add_run(val)
        r_v.font.size = Pt(10.5)
        if label == "SHOW THEME:":
            r_v.italic = True
            
        set_cell_margins(cell_lbl, top=60, bottom=60, left=60, right=60)
        set_cell_margins(cell_val, top=60, bottom=60, left=60, right=60)
        
    doc.add_page_break()
    
    # Helper for headings
    def add_custom_heading(text, level):
        h = doc.add_heading(text, level=level)
        h.paragraph_format.space_before = Pt(14)
        h.paragraph_format.space_after = Pt(6)
        r = h.runs[0]
        if level == 1:
            r.font.size = Pt(16)
            r.font.color.rgb = RGBColor(0x00, 0x33, 0x99) # Royal Blue
            r.bold = True
        elif level == 2:
            r.font.size = Pt(13)
            r.font.color.rgb = RGBColor(0x0D, 0x47, 0xA1)
            r.bold = True
        elif level == 3:
            r.font.size = Pt(11.5)
            r.font.color.rgb = RGBColor(0x33, 0x33, 0x33)
            r.bold = True
        return h

    # --- EXECUTIVE SUMMARY ---
    add_custom_heading("EXECUTIVE SUMMARY", level=1)
    
    p_exec = doc.add_paragraph()
    p_exec.paragraph_format.line_spacing = 1.15
    p_exec.paragraph_format.space_after = Pt(10)
    p_exec.add_run(
        "The smallholder pig farming sector in Kenya plays a vital role in food security, employment generation, and economic empowerment. "
        "However, pig farmers face severe operational bottlenecks, including unorganized paper-based record-keeping, high feed prices (accounting for 70–80% of total production costs), "
        "untracked disease outbreaks, poor breeding management, and lack of real-time financial transparency.\n\n"
        "The Digital Pig Farm Manager is an offline-first mobile Android application developed in Kotlin using modern Jetpack Compose UI, Room SQLite database, and automated analytics engine. "
        "The system empowers farmers to manage individual pig lifecycles, log health and vaccination events for single pigs or entire categories (weaners, growers, finishers, boars, sows), "
        "record buyer transactions at point of sale for direct market linkage, formulate least-cost feed rations, and generate dynamic reports covering Feed Conversion Ratio (FCR), "
        "Average Daily Gain (ADG), disease incidence rates, farrowing efficiency, and Profit & Loss (P&L) performance.\n\n"
        "This project directly aligns with the Kitale ASK Show 2026 theme by introducing digital climate-smart technology to optimize resource utilization, reduce feed wastage, "
        "prevent livestock mortality, and promote commercial agribusiness sustainability."
    )
    
    # --- TABLE OF CONTENTS ---
    add_custom_heading("TABLE OF CONTENTS", level=1)
    toc_items = [
        "1. Cover Page & Executive Summary",
        "2. Chapter 1: Introduction & Research Background",
        "   1.1 Background of the Study",
        "   1.2 Problem Statement",
        "   1.3 Research Questions",
        "   1.4 Objectives of the Project (Main & Specific Objectives)",
        "   1.5 Justification and Significance of the System",
        "   1.6 Scope and Delimitations",
        "3. Chapter 2: System Features & Visual Breakdown",
        "   2.1 Core System Capabilities",
        "   2.2 Market Connection & Sales Flow Workflow",
        "4. Chapter 3: System Design & Methodology",
        "   3.1 Development Methodology (Agile Scrum)",
        "   3.2 System Architecture (MVVM & Clean Architecture)",
        "   3.3 Database Schema & Entity Design",
        "   3.4 Key Mathematical & Analytical Algorithms (FCR, ADG, P&L)",
        "5. Project Budget & Resource Allocation (Total Ksh 5,000.00)",
        "6. Chapter 4: Conclusion & Recommendations",
        "7. References (APA Style)"
    ]
    for item in toc_items:
        p_t = doc.add_paragraph()
        p_t.paragraph_format.space_after = Pt(2)
        r = p_t.add_run(item)
        r.font.size = Pt(10.5)
        if item.strip().startswith(("1.", "2.", "3.", "4.", "5.", "6.", "7.")):
            r.bold = True
            r.font.color.rgb = RGBColor(0x00, 0x33, 0x99)
            
    doc.add_page_break()

    # --- CHAPTER 1 ---
    add_custom_heading("CHAPTER 1: INTRODUCTION & RESEARCH BACKGROUND", level=1)
    
    add_custom_heading("1.1 Background of the Study", level=2)
    p_bg = doc.add_paragraph()
    p_bg.paragraph_format.line_spacing = 1.15
    p_bg.paragraph_format.space_after = Pt(8)
    p_bg.add_run(
        "Sub-Saharan Africa has witnessed rapid growth in pig production over the past decade, driven by urban population growth, changing dietary habits, "
        "and increasing demand for pork products. In Kenya, pig farming offers high feed-to-meat conversion efficiency, short gestation periods (114 days), "
        "and prolific litter sizes, making it an attractive enterprise for smallholder farmers and youth entrepreneurs.\n\n"
        "Despite this potential, swine farming in Kenya remains predominantly informal. Most smallholders rely on memory or physical notebooks to track mating dates, "
        "farrowing schedules, feed consumption, mortality rates, and medical treatments. This lack of structured data leads to severe management deficiencies."
    )
    
    add_custom_heading("1.2 Problem Statement", level=2)
    p_prob = doc.add_paragraph()
    p_prob.paragraph_format.line_spacing = 1.15
    p_prob.paragraph_format.space_after = Pt(8)
    p_prob.add_run(
        "Traditional pig farming is crippled by manual record-keeping systems and a lack of real-time analytical tools. Farmers are unable to: "
        "(1) accurately monitor individual pig health histories and category-wide disease trends; (2) compute biological performance metrics like FCR and ADG; "
        "(3) link pig sales directly with buyer contact records (butcheries, processors, traders); and (4) generate consolidated Profit & Loss reports."
    )

    add_custom_heading("1.3 Research Questions", level=2)
    rqs = [
        "How can mobile technology replace manual record-keeping to enhance accuracy in pig health and breeding tracking?",
        "How can sales workflows allow farmers to directly add and track buyer contacts during transactions?",
        "How can automated algorithms assist farmers in computing FCR, ADG, feed formulation, and net profit margins?"
    ]
    for q in rqs:
        p_q = doc.add_paragraph(style='List Bullet')
        p_q.paragraph_format.space_after = Pt(4)
        p_q.add_run(q)

    add_custom_heading("1.4 Objectives of the Project", level=2)
    add_custom_heading("1.4.1 Main Objective", level=3)
    p_mo = doc.add_paragraph()
    p_mo.add_run("To design, develop, and deploy an offline-first mobile Android application—Digital Pig Farm Manager—that automates swine herd tracking, veterinary health logging, market buyer sales logging, least-cost feed formulation, and financial/production reporting.")

    add_custom_heading("1.4.2 Specific Objectives", level=3)
    objs = [
        "To implement an individual and group pig tracking module supporting biological stages (piglets, weaners, growers, finishers, boars, sows), lineage, and weight history.",
        "To build a robust health and veterinary event logging system capable of recording single-pig, category-wide, or herd-wide medical interventions, gilt servicing, disease diagnoses, age, weight, and costs.",
        "To design a sales and market linkage module enabling farmers to capture buyer details (name, contact, buyer category like butchery, processor, trader) at the point of sale.",
        "To integrate a least-cost feed formulator and inventory tracker supporting commercial premix allocation and exact kg batch distribution.",
        "To construct an interactive Reports Hub featuring custom date-range filtering (Today, Last 7 Days, Last 30 Days, YTD, All Time) for Financial P&L, Herd Production, Health/Mortality, Feed & Growth (FCR/ADG), and Breeding performance."
    ]
    for o in objs:
        p_o = doc.add_paragraph(style='List Bullet')
        p_o.paragraph_format.space_after = Pt(4)
        p_o.add_run(o)

    add_custom_heading("1.5 Justification & Significance", level=2)
    p_j = doc.add_paragraph()
    p_j.add_run("The system boosts smallholder profitability by reducing feed wastage, preventing livestock mortality, organizing buyer histories, and offering verifiable digital financial records for agribusiness sustainability.")

    add_custom_heading("1.6 Scope & Delimitations", level=2)
    p_s = doc.add_paragraph()
    p_s.add_run("Scope covers Android mobile development (Kotlin/Jetpack Compose, Room SQLite) with 100% offline capability.")

    # --- CHAPTER 2 ---
    add_custom_heading("CHAPTER 2: SYSTEM FEATURES & VISUAL BREAKDOWN", level=1)
    add_custom_heading("2.1 Core System Capabilities", level=2)
    caps = [
        "Herd Registry: Individual pig profiles, tag numbers, lineage tracking, stage history, and weight logs.",
        "Vet & Health Logging: Flexible target scope (Single Pig, Category, All Herd), Gilt servicing, vaccination logs, disease diagnostics, age (weeks), weight (kg), and expenses.",
        "Market Connection & Sales Flow: Farmers record buyer contact details and buyer type (butchery, processor, trader) directly when logging pig sales.",
        "Least-Cost Feed Formulator: Precision ingredient distribution, premix stock tracking, and batch target calculators.",
        "Reports Hub: Filterable reports for Financial P&L, Herd Inventory, Health/Mortality, Feed FCR/ADG, and Breeding efficiency."
    ]
    for c in caps:
        p_c = doc.add_paragraph(style='List Bullet')
        p_c.paragraph_format.space_after = Pt(4)
        p_c.add_run(c)

    # --- CHAPTER 3 ---
    add_custom_heading("CHAPTER 3: SYSTEM DESIGN & METHODOLOGY", level=1)
    add_custom_heading("3.1 Development Methodology", level=2)
    p_m = doc.add_paragraph()
    p_m.add_run("The project adopted Agile Scrum methodology across 5 iterative sprints: Database schema design, UI & Herd registry, Health logging & Formulator, Reports Hub & Sales flow, and Device testing & GitHub deployment.")

    add_custom_heading("3.2 System Architecture", level=2)
    p_a = doc.add_paragraph()
    p_a.add_run("Built using official Android Model-View-ViewModel (MVVM) clean architecture: Jetpack Compose for UI, MainViewModel with Kotlin StateFlow for reactive state, PHMSRepository, and Room SQLite Database for local persistence.")

    add_custom_heading("3.3 Database Schema", level=2)
    p_db = doc.add_paragraph()
    p_db.add_run("The SQLite database comprises core entities: PigEntity, HealthEventEntity, FeedStockEntity, FeedFormulationEntity, BreedingRecordEntity, SalesEntity (including buyer details), and ExpenseEntity.")

    add_custom_heading("3.4 Key Mathematical Algorithms", level=2)
    p_alg = doc.add_paragraph()
    p_alg.paragraph_format.line_spacing = 1.15
    p_alg.add_run("• Feed Conversion Ratio (FCR) = Total Feed Consumed (kg) / Total Weight Gained (kg)\n")
    p_alg.add_run("• Average Daily Gain (ADG) = (Final Weight - Initial Weight) / Time Period (Days)\n")
    p_alg.add_run("• Net Profit (Ksh) = Sales Revenue - (Feed Cost + Health Cost + Operating Expenses)")

    # --- BUDGET TABLE ---
    add_custom_heading("PROJECT BUDGET & RESOURCE ALLOCATION", level=1)
    p_b_intro = doc.add_paragraph()
    p_b_intro.add_run("The total development budget for the project was strictly managed and optimized to Ksh 5,000.00:")
    
    t_budget = doc.add_table(rows=7, cols=5)
    t_budget.alignment = WD_TABLE_ALIGNMENT.CENTER
    t_budget.autofit = False
    
    headers = ["Item No.", "Description / Resource", "Quantity / Scope", "Unit Cost (Ksh)", "Total Cost (Ksh)"]
    row_hdr = t_budget.rows[0]
    for idx, h_text in enumerate(headers):
        cell = row_hdr.cells[idx]
        set_cell_background(cell, "003399") # KNP Royal Blue header
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        r = p.add_run(h_text)
        r.bold = True
        r.font.size = Pt(9.5)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)
        set_cell_margins(cell, top=80, bottom=80, left=80, right=80)
        
    b_data = [
        ("1", "Data & Internet Connectivity (Research, Gradle & GitHub)", "2 Months", "1,000.00", "1,000.00"),
        ("2", "Field Data Collection & Farmer Logistics", "3 Trips", "500.00", "1,500.00"),
        ("3", "Hardware Testing & Device Setup (OTG cable, ADB debug)", "1 Set", "800.00", "800.00"),
        ("4", "Documentation & Exhibition Materials (Printing & Binding)", "1 Package", "1,200.00", "1,200.00"),
        ("5", "Contingency & Show Logistics (Stationery & Flash Drive)", "Lump Sum", "500.00", "500.00"),
        ("TOTAL", "GRAND TOTAL PROJECT BUDGET", "", "", "Ksh 5,000.00")
    ]
    
    col_widths = [Inches(0.7), Inches(2.7), Inches(1.1), Inches(1.0), Inches(1.1)]
    
    for r_idx, b_row in enumerate(b_data):
        row = t_budget.rows[r_idx + 1]
        is_total = (r_idx == len(b_data) - 1)
        for c_idx, val in enumerate(b_row):
            cell = row.cells[c_idx]
            cell.width = col_widths[c_idx]
            if is_total:
                set_cell_background(cell, "E8EAF6")
            elif r_idx % 2 == 1:
                set_cell_background(cell, "F5F5F5")
            p = cell.paragraphs[0]
            if c_idx in [0, 2, 3, 4]:
                p.alignment = WD_ALIGN_PARAGRAPH.RIGHT if c_idx >= 3 else WD_ALIGN_PARAGRAPH.CENTER
            r = p.add_run(val)
            r.font.size = Pt(9.5)
            if is_total:
                r.bold = True
                r.font.color.rgb = RGBColor(0x00, 0x33, 0x99)
            set_cell_margins(cell, top=60, bottom=60, left=60, right=60)

    # --- CHAPTER 4 ---
    add_custom_heading("CHAPTER 4: CONCLUSION & RECOMMENDATIONS", level=1)
    add_custom_heading("4.1 Conclusion", level=2)
    p_conc = doc.add_paragraph()
    p_conc.add_run("The Digital Pig Farm Manager successfully bridges the digital gap in smallholder pig farming. By providing offline-first records, least-cost feed formulation, direct sales buyer tracking, and automated P&L analytics, the system advances climate-smart, tech-driven commercial agriculture.")

    add_custom_heading("4.2 Recommendations & Future Work", level=2)
    p_rec = doc.add_paragraph()
    p_rec.add_run("Future iterations will introduce Bluetooth scale sensor integration, AI-powered disease image recognition, and cloud USSD fallback for non-smartphone users.")

    # --- REFERENCES ---
    add_custom_heading("REFERENCES", level=1)
    refs = [
        "1. Food and Agriculture Organization (FAO). (2022). African Swine Fever: Detection and Diagnosis - A manual for veterinarians. FAO Animal Production and Health Manual No. 19. Rome.",
        "2. Ministry of Agriculture, Livestock and Fisheries, Kenya. (2021). National Livestock Policy & Pig Production Guidelines. Government Printer, Nairobi.",
        "3. Agricultural Society of Kenya (ASK). (2026). Kitale National Show Exhibition Catalogue and Theme Guidelines. Kitale, Kenya.",
        "4. Android Developers. (2024). Modern Android Development with Jetpack Compose & Room Database Architecture. Google Open Source Guidelines.",
        "5. Edwards, S. A. (2019). Productivity and Efficiency Metrics in Smallholder Swine Farming in East Africa. Journal of Tropical Animal Health and Production, 51(4), 887-895."
    ]
    for ref in refs:
        p_ref = doc.add_paragraph()
        p_ref.paragraph_format.space_after = Pt(6)
        r = p_ref.add_run(ref)
        r.font.size = Pt(9.5)
        r.font.color.rgb = RGBColor(0x42, 0x42, 0x42)

    doc.save("Kitale_ASK_Show_2026_Project_Report.docx")
    print("Successfully generated Kitale_ASK_Show_2026_Project_Report.docx!")

if __name__ == "__main__":
    create_docx()
