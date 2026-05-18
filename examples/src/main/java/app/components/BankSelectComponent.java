package app.components;

import app.model.Bank;
import app.services.BrazilApiBankClient;
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.util.List;
import java.util.stream.Collectors;

import static ujfe.core.UI.*;

public final class BankSelectComponent implements Component {
    private final BrazilApiBankClient bankClient;
    private final Signal<List<Bank>> banks;
    private final Signal<String> status;

    public BankSelectComponent() {
        this(new BrazilApiBankClient());
    }

    BankSelectComponent(BrazilApiBankClient bankClient) {
        this.bankClient = bankClient;
        this.banks = Signals.signal(BrazilApiBankClient.fallbackBanks());
        this.status = Signals.signal("Lista inicial local. Clique para consultar a BrasilAPI.");
    }

    @Override
    public Node render() {
        return div()
                .css("rounded-lg border border-secondary-200 bg-secondary-50 p-4 shadow-sm flex flex-col gap-3")
                .child(h3("Select preenchido por API REST").css("text-lg font-bold text-secondary-700"))
                .child(p(() -> status.get()).css("text-sm text-slate-700 leading-relaxed"))
                .child(
                        select()
                                .name("bank")
                                .css("w-full rounded border border-secondary-200 bg-white p-2 text-sm")
                                .children(banks.get().stream()
                                        .map(bank -> option(bank.label()).value(bank.selectValue()))
                                        .collect(Collectors.toList()))
                )
                .child(
                        button("Carregar bancos da BrasilAPI")
                                .css("px-4 py-2 rounded bg-secondary-700 text-white font-semibold")
                                .onClick(this::loadBanks)
                )
                .child(p("Endpoint usado: https://brasilapi.com.br/api/banks/v1")
                        .css("text-xs text-slate-600 font-mono"));
    }

    private void loadBanks() {
        try {
            List<Bank> loadedBanks = bankClient.fetchBanks();
            banks.set(loadedBanks);
            status.set("Consulta REST concluida. Bancos carregados: " + loadedBanks.size() + ".");
        } catch (RuntimeException exception) {
            banks.set(BrazilApiBankClient.fallbackBanks());
            status.set("Nao foi possivel consultar a API agora. Mantendo a lista local de exemplo.");
        }
    }
}
