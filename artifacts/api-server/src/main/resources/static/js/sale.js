(() => {
    const form = document.querySelector("#sale-form");
    if (!form) return;

    const inputs = [...form.querySelectorAll(".sale-quantity")];
    const totalDisplay = document.querySelector("#sale-total");

    const toCents = (value) => {
        const [whole = "0", fraction = ""] = String(value).split(".");
        return BigInt(whole || "0") * 100n + BigInt((fraction + "00").slice(0, 2));
    };

    const formatCents = (cents) => {
        const whole = cents / 100n;
        const fraction = String(cents % 100n).padStart(2, "0");
        return `R$ ${new Intl.NumberFormat("pt-BR").format(whole)},${fraction}`;
    };

    const updateTotal = () => {
        const cents = inputs.reduce((total, input) => {
            const card = input.closest(".sale-product");
            const priceCents = toCents(card.dataset.price);
            const quantity = BigInt(Math.max(0, Number.parseInt(input.value || "0", 10)));
            return total + priceCents * quantity;
        }, 0n);
        totalDisplay.textContent = formatCents(cents);
    };

    form.querySelectorAll(".quantity-step").forEach((button) => {
        button.addEventListener("click", () => {
            const input = button.parentElement.querySelector(".sale-quantity");
            const current = Number.parseInt(input.value || "0", 10);
            const max = Number.parseInt(input.max || "999999", 10);
            input.value = String(Math.min(max, Math.max(0, current + Number(button.dataset.step))));
            input.dispatchEvent(new Event("input", { bubbles: true }));
        });
    });

    inputs.forEach((input) => {
        input.addEventListener("input", () => {
            const max = Number.parseInt(input.max || "999999", 10);
            const value = Number.parseInt(input.value || "0", 10);
            if (value > max) input.value = String(max);
            if (value < 0) input.value = "0";
            updateTotal();
        });
    });

    const dateInput = document.querySelector("#saleDate");
    dateInput?.addEventListener("change", () => {
        const newDate = dateInput.value.slice(0, 10);
        const headingDate = document.querySelector(".section-meta");
        if (!newDate || !headingDate) return;
        const action = form.getAttribute("action");
        const basePath = action.replace(/\/sale\/?$/, "");
        const target = new URL(`${basePath}/sale/date`, window.location.origin);
        target.searchParams.set("date", newDate);
        if (dateInput.value.slice(0, 10) !== dateInput.dataset.loadedDate) {
            window.location.assign(target.toString());
        }
    });

    dateInput.dataset.loadedDate = dateInput.value.slice(0, 10);
    const keptDate = document.querySelector("#kept-date")?.textContent;
    if (keptDate && dateInput) dateInput.value = keptDate;
    updateTotal();
})();
